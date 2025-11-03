package eu.netward.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class ResponseCache {
    
    private static final Logger logger = LoggerFactory.getLogger(ResponseCache.class);
    
    private final Cache<String, CacheEntry> cache;
    
    public ResponseCache(long maxWeightBytes, long maxAgeSeconds) {
        this.cache = Caffeine.newBuilder()
            .maximumWeight(maxWeightBytes)
            .weigher((Weigher<String, CacheEntry>) (key, value) -> value.getWeight())
            .expireAfterWrite(Duration.ofSeconds(maxAgeSeconds))
            .recordStats()
            .removalListener((key, value, cause) -> {
                if (value != null) {
                    logger.debug("Cache entry evicted: {} ({} bytes, age: {}s, reason: {})", 
                        key, value.getBody().length(), value.getAgeSeconds(), cause);
                }
            })
            .build();
    }
    
    public void put(String key, CacheEntry entry) {
        cache.put(key, entry);
        logger.info("Cached: {} ({} bytes, TTL: {}s)", 
            key, entry.getBody().length(), entry.getTtlSeconds());
    }
    
    public CacheEntry get(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry != null && entry.isStale()) {
            logger.debug("Cache entry stale, invalidating: {}", key);
            cache.invalidate(key);
            return null;
        }
        return entry;
    }
    
    public void invalidate(String key) {
        cache.invalidate(key);
        logger.info("Invalidated cache entry: {}", key);
    }
    
    public void invalidateAll() {
        cache.invalidateAll();
        logger.info("Cleared all cache entries");
    }
    
    public void invalidatePattern(String pattern) {
        long count = cache.asMap().keySet().stream()
            .filter(key -> key.matches(pattern))
            .peek(cache::invalidate)
            .count();
        logger.info("Purged {} cache entries matching pattern: {}", count, pattern);
    }
    
    public long size() {
        return cache.estimatedSize();
    }
    
    public CacheStats getStats() {
        return cache.stats();
    }
    
    public void logStats() {
        CacheStats stats = cache.stats();
        logger.info("Cache stats - Size: {}, Hits: {}, Misses: {}, Hit rate: {:.2f}%, Evictions: {}, Load failures: {}", 
            cache.estimatedSize(),
            stats.hitCount(), 
            stats.missCount(),
            stats.hitRate() * 100,
            stats.evictionCount(),
            stats.loadFailureCount());
    }
}
