package eu.netward.cache;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

public class CacheEntry {
    
    private final int statusCode;
    private final MultiMap headers;
    private final Buffer body;
    private final long cacheTime;
    private final long ttlSeconds;
    private final String etag;
    
    public CacheEntry(int statusCode, MultiMap headers, Buffer body, long ttlSeconds) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.cacheTime = System.currentTimeMillis();
        this.ttlSeconds = ttlSeconds;
        this.etag = headers.get("ETag");
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public MultiMap getHeaders() {
        return headers;
    }
    
    public Buffer getBody() {
        return body;
    }
    
    public long getCacheTime() {
        return cacheTime;
    }
    
    public long getTtlSeconds() {
        return ttlSeconds;
    }
    
    public String getETag() {
        return etag;
    }
    
    public boolean isStale() {
        return System.currentTimeMillis() - cacheTime > (ttlSeconds * 1000);
    }
    
    public long getAgeSeconds() {
        return (System.currentTimeMillis() - cacheTime) / 1000;
    }
    
    public int getWeight() {
        // Calculate weight for cache eviction (body size + header overhead)
        return body.length() + 1024;
    }
}
