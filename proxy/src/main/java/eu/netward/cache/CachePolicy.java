package eu.netward.cache;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpClientResponse;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CachePolicy {
    
    private static final Pattern CACHE_CONTROL_MAX_AGE = Pattern.compile("max-age=(\\d+)");
    
    private static final Set<String> CACHEABLE_CONTENT_TYPES = Set.of(
        "text/css", 
        "text/javascript", 
        "application/javascript", 
        "application/x-javascript",
        "image/jpeg", 
        "image/jpg", 
        "image/png", 
        "image/gif", 
        "image/webp", 
        "image/svg+xml", 
        "image/x-icon", 
        "image/vnd.microsoft.icon",
        "font/woff", 
        "font/woff2", 
        "font/ttf", 
        "font/otf", 
        "font/eot",
        "application/font-woff", 
        "application/font-woff2", 
        "application/x-font-ttf",
        "application/x-font-opentype", 
        "application/vnd.ms-fontobject",
        "video/mp4", 
        "video/webm", 
        "audio/mpeg", 
        "audio/ogg",
        "application/pdf"
    );
    
    private static final Set<String> CACHEABLE_EXTENSIONS = Set.of(
        ".css", ".js", ".mjs",
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".ico",
        ".woff", ".woff2", ".ttf", ".otf", ".eot",
        ".mp4", ".webm", ".mp3", ".ogg", ".pdf"
    );
    
    private final long maxTtlSeconds;
    private final long maxCacheableSizeBytes;
    
    public CachePolicy(long maxTtlSeconds, long maxCacheableSizeBytes) {
        this.maxTtlSeconds = maxTtlSeconds;
        this.maxCacheableSizeBytes = maxCacheableSizeBytes;
    }
    
    public boolean isCacheable(HttpServerRequest request, HttpClientResponse response) {
        // Only cache GET and HEAD requests
        String method = request.method().name();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return false;
        }
        
        // Only cache successful responses
        if (response.statusCode() != 200) {
            return false;
        }
        
        // Check Cache-Control from origin
        String cacheControl = response.getHeader("Cache-Control");
        if (cacheControl != null) {
            if (cacheControl.contains("no-cache") || 
                cacheControl.contains("no-store") || 
                cacheControl.contains("private")) {
                return false;
            }
        }
        
        // Check if content length exceeds max cacheable size
        String contentLength = response.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                long size = Long.parseLong(contentLength);
                if (size > maxCacheableSizeBytes) {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Ignore and continue
            }
        }
        
        // Check content type
        String contentType = response.getHeader("Content-Type");
        if (contentType != null) {
            for (String cacheableType : CACHEABLE_CONTENT_TYPES) {
                if (contentType.toLowerCase().startsWith(cacheableType)) {
                    return true;
                }
            }
        }
        
        // Check file extension
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        if (queryIndex != -1) {
            uri = uri.substring(0, queryIndex);
        }
        
        for (String ext : CACHEABLE_EXTENSIONS) {
            if (uri.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if request has cache-busting headers that should bypass cache.
     * Unlike browsers, we only honor no-store (not no-cache) for static assets,
     * similar to Cloudflare's behavior.
     */
    public boolean hasCacheBustingHeaders(HttpServerRequest request) {
        String cacheControl = request.getHeader("Cache-Control");
        
        // Only bypass cache for no-store (legal requirement), not no-cache
        if (cacheControl != null && cacheControl.contains("no-store")) {
            return true;
        }
        
        return false;
    }
    
    public long calculateTTL(HttpClientResponse response) {
        // Check Cache-Control max-age
        String cacheControl = response.getHeader("Cache-Control");
        if (cacheControl != null) {
            Matcher matcher = CACHE_CONTROL_MAX_AGE.matcher(cacheControl);
            if (matcher.find()) {
                try {
                    long maxAge = Long.parseLong(matcher.group(1));
                    return Math.min(maxAge, maxTtlSeconds);
                } catch (NumberFormatException e) {
                    // Ignore and fall through to defaults
                }
            }
        }
        
        // Default TTL based on content type
        String contentType = response.getHeader("Content-Type");
        if (contentType != null) {
            if (contentType.startsWith("image/") || contentType.startsWith("font/") || contentType.startsWith("video/")) {
                return Math.min(7200, maxTtlSeconds); // 2 hours for media
            }
            if (contentType.contains("javascript") || contentType.contains("css")) {
                return Math.min(3600, maxTtlSeconds); // 1 hour for JS/CSS
            }
        }
        
        return Math.min(1800, maxTtlSeconds); // 30 minutes default
    }
    
    public String buildCacheKey(String host, String uri) {
        // Normalize the URI (remove fragment)
        int fragmentIndex = uri.indexOf('#');
        if (fragmentIndex != -1) {
            uri = uri.substring(0, fragmentIndex);
        }
        return host + ":" + uri;
    }
    
    public long getMaxCacheableSizeBytes() {
        return maxCacheableSizeBytes;
    }
}
