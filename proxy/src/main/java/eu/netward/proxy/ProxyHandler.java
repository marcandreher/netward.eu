package eu.netward.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final HostHandler hostHandler;

    public ProxyHandler() {
        this.templateEngine = TemplateEngine.createPrecompiled(ContentType.Html);
        this.netwardPrefix = System.getenv().getOrDefault("NETWARD_PREFIX", "NONE");
        this.hostHandler = new HostHandler();
    }

    public void handleProxy(HttpServerRequest req, HttpClient client) {
        String hostHeader = req.getHeader("host");
        String requestId = RequestIdGenerator.generate(netwardPrefix);

        var zone = hostHandler.getZoneForHost(hostHeader);

        if (zone == null) {
            logger.warn("Blocked request to unauthorized host: " + hostHeader);

            StatusTemplateHandler.handle(templateEngine, req, HttpStatus.FORBIDDEN, "This host is not part of netward network.", requestId);
            return;
        }

        String targetHost = zone.getTarget();
        int targetPort = 80;
        
        long startTime = System.currentTimeMillis();

        logger.info("Proxying request: " + req.method() + " " + req.uri() + " from " + hostHeader + " to " + targetHost + ":" + targetPort);

        // Create the proxy request
        client.request(req.method(), targetPort, targetHost, req.uri())
            .onSuccess(proxyReq -> {
                // Copy headers
                proxyReq.headers().setAll(req.headers());
                proxyReq.headers().set("Host", hostHeader);
                
                // Setup response handler FIRST
                proxyReq.response()
                    .onSuccess(proxyRes -> {
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("Received response: " + proxyRes.statusCode() + " in " + duration + " ms");
                        
                        HttpServerResponse clientRes = req.response();
                        clientRes.setStatusCode(proxyRes.statusCode());
                        clientRes.headers().setAll(proxyRes.headers());
                        clientRes.headers().add("NW-RequestID", requestId);
                        
                        // Stream the response directly without buffering
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
}
