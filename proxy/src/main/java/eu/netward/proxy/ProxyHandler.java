package eu.netward.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netward.cache.CacheEntry;
import eu.netward.cache.CachePolicy;
import eu.netward.cache.ResponseCache;
import eu.netward.model.HttpStatus;
import eu.netward.util.RequestIdGenerator;
import eu.netward.web.StatusTemplateHandler;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class ProxyHandler {

    private final Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final TemplateEngine templateEngine;
    private final String netwardPrefix;
    private final String netwardPublicIp;
    private final HostHandler hostHandler;
    private final ResponseCache responseCache;
    private final CachePolicy cachePolicy;

    public ProxyHandler() {
        this.templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
        this.netwardPrefix = System.getenv().getOrDefault("NETWARD_PREFIX", "NONE");
        this.netwardPublicIp = System.getenv().getOrDefault("NETWARD_PUBLIC_IP", "NONE");
        this.hostHandler = new HostHandler();
        
        // Initialize cache with 512MB max size and 4 hour max TTL
        this.responseCache = new ResponseCache(512 * 1024 * 1024, 4 * 3600);
        
        // Initialize cache policy with 4 hour max TTL and 10MB max item size
        this.cachePolicy = new CachePolicy(4 * 3600, 10 * 1024 * 1024);
    }

    public void handleProxy(HttpServerRequest req, HttpClient client) {
        String hostHeader = req.getHeader("host");
        String requestId = RequestIdGenerator.generate(netwardPrefix);

        if(hostHeader == null || hostHeader.equals(netwardPublicIp)) {
            logger.warn("Blocked direct ip access: " + hostHeader);

            StatusTemplateHandler.handle(templateEngine, req, HttpStatus.FORBIDDEN, "Direct IP access is not allowed.", requestId);
            return;
        }

        var zone = hostHandler.getZoneForHost(hostHeader);

        if (zone == null) {
            logger.warn("Blocked request to unauthorized host: " + hostHeader);

            StatusTemplateHandler.handle(templateEngine, req, HttpStatus.FORBIDDEN, "This host is not part of netward network.", requestId);
            return;
        }

        String targetHost = zone.getTarget();
        int targetPort = 80;
        
        // Build cache key
        String cacheKey = cachePolicy.buildCacheKey(hostHeader, req.uri());
        
        // Check if request has cache-busting headers
        if (cachePolicy.hasCacheBustingHeaders(req)) {
            logger.debug("Cache-busting headers detected, bypassing cache for: {}", cacheKey);
            proxyRequest(req, client, targetHost, targetPort, hostHeader, requestId, null);
            return;
        }
        
        // Try to serve from cache (only for GET/HEAD)
        String method = req.method().name();
        if ("GET".equals(method) || "HEAD".equals(method)) {
            CacheEntry cached = responseCache.get(cacheKey);
            if (cached != null) {
                // Handle conditional requests (If-None-Match with ETag)
                String ifNoneMatch = req.getHeader("If-None-Match");
                if (ifNoneMatch != null && cached.getETag() != null && ifNoneMatch.equals(cached.getETag())) {
                    logger.info("Cache HIT (304 Not Modified): {}", cacheKey);
                    HttpServerResponse res = req.response();
                    res.setStatusCode(304);
                    res.headers().set("NW-RequestID", requestId);
                    res.headers().set("X-Cache", "HIT");
                    res.headers().set("Age", String.valueOf(cached.getAgeSeconds()));
                    if (cached.getETag() != null) {
                        res.headers().set("ETag", cached.getETag());
                    }
                    res.end();
                    return;
                }
                
                // Serve from cache
                logger.info("Cache HIT: {} (age: {}s)", cacheKey, cached.getAgeSeconds());
                HttpServerResponse res = req.response();
                res.setStatusCode(cached.getStatusCode());
                res.headers().setAll(cached.getHeaders());
                res.headers().set("NW-RequestID", requestId);
                res.headers().set("X-Cache", "HIT");
                res.headers().set("Age", String.valueOf(cached.getAgeSeconds()));
                res.headers().set("Cache-Control", "public, max-age=" + cached.getTtlSeconds());
                res.end(cached.getBody());
                return;
            }
        }
        
        // Cache miss - proxy the request
        proxyRequest(req, client, targetHost, targetPort, hostHeader, requestId, cacheKey);
    }
    
    private void proxyRequest(HttpServerRequest req, HttpClient client, String targetHost, 
                              int targetPort, String hostHeader, String requestId, String cacheKey) {
        long startTime = System.currentTimeMillis();

        logger.info("Proxying request: " + req.method() + " " + req.uri() + " from " + hostHeader + " to " + targetHost + ":" + targetPort);

        // Create the proxy request
        client.request(req.method(), targetPort, targetHost, req.uri())
            .onSuccess(proxyReq -> {
                // Set headers
                proxyReq.headers().setAll(req.headers());
                proxyReq.headers().set("Host", hostHeader);
                proxyReq.headers().set("X-Real-IP", req.remoteAddress().host());
                proxyReq.headers().set("X-Forwarded-For", req.remoteAddress().host());
                proxyReq.headers().set("X-Forwarded-Proto", req.scheme());
                
                // Setup response handler FIRST
                proxyReq.response()
                    .onSuccess(proxyRes -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("Received response: " + proxyRes.statusCode() + " in " + duration + " ms");
                        
                        HttpServerResponse clientRes = req.response();
                        clientRes.setStatusCode(proxyRes.statusCode());
                        clientRes.headers().setAll(proxyRes.headers());
                        clientRes.headers().set("NW-RequestID", requestId);
                        clientRes.headers().set("X-Cache", "MISS");
                        
                        // Determine if response should be cached
                        boolean shouldCache = cacheKey != null && cachePolicy.isCacheable(req, proxyRes);
                        
                        if (shouldCache) {
                            // Buffer the response to cache it
                            proxyRes.body()
                                .onSuccess(body -> {
                                    if (body.length() <= cachePolicy.getMaxCacheableSizeBytes()) {
                                        long ttl = cachePolicy.calculateTTL(proxyRes);
                                        CacheEntry entry = new CacheEntry(
                                            proxyRes.statusCode(),
                                            proxyRes.headers(),
                                            body,
                                            ttl
                                        );
                                        responseCache.put(cacheKey, entry);
                                    }
                                    clientRes.end(body);
                                })
                                .onFailure(err -> {
                                    logger.error("Failed to buffer response: " + err.getMessage());
                                    if (!clientRes.ended()) {
                                        clientRes.setStatusCode(502).end("Failed to read upstream response");
                                    }
                                });
                        } else {
                            // Stream non-cacheable responses
                            proxyRes.pipeTo(clientRes)
                                .onSuccess(v -> {
                                    logger.info("Request completed successfully in " + (System.currentTimeMillis() - startTime) + " ms");
                                })
                                .onFailure(err -> {
                                    logger.error("Failed to pipe response: " + err.getMessage());
                                    if (!clientRes.ended()) {
                                        clientRes.setStatusCode(502).end("Failed to read upstream response");
                                    }
                                });
                        }
                    })
                    .onFailure(err -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.error("Proxy response failed after " + duration + " ms: " + err.getMessage());
                        if (!req.response().ended()) {
                            StatusTemplateHandler.handle(templateEngine, req, HttpStatus.BAD_GATEWAY, "Failed to get response: " + err.getMessage(), requestId);
                        }
                    });
                
                // THEN forward the request body - this is key!
                // Use a pump to forward data as it arrives
                req.pipeTo(proxyReq)
                    .onFailure(err -> {
                        logger.error("Failed to pipe request: " + err.getMessage());
                        proxyReq.reset();
                        if (!req.response().ended()) {
                            StatusTemplateHandler.handle(templateEngine, req, HttpStatus.BAD_GATEWAY, "Failed to forward request: " + err.getMessage(), requestId);
                        }
                    });
                
                // Resume the request so data starts flowing
                req.resume();
            })
            .onFailure(err -> {
                long duration = System.currentTimeMillis() - startTime;
                logger.error("Proxy request failed after " + duration + " ms: " + err.getMessage());
                if (!req.response().ended()) {
                    StatusTemplateHandler.handle(templateEngine, req, HttpStatus.BAD_GATEWAY, "The upstream server is unreachable", requestId);
                }
            });
    }
    
    /**
     * Log cache statistics
     */
    public void logCacheStats() {
        responseCache.logStats();
    }
    
    /**
     * Invalidate all cache entries
     */
    public void clearCache() {
        responseCache.invalidateAll();
    }
    
    /**
     * Invalidate cache entries matching a pattern
     */
    public void purgeCachePattern(String pattern) {
        responseCache.invalidatePattern(pattern);
    }
}
