package eu.netward.proxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import eu.netward.App;
import eu.netward.model.NetwardZone;

public class HostHandler {

    private final Logger logger = LoggerFactory.getLogger(HostHandler.class);
    private final Cache<String, NetwardZone> hostCache;
    
    // Sentinel value to represent "not found" in cache
    private static final NetwardZone NOT_FOUND = new NetwardZone();

    public HostHandler() {
        this.hostCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    public NetwardZone getZoneForHost(String host) {
        long startTime = System.currentTimeMillis();

        if(host == null || host.isEmpty()) {
            return null;
        }

        if(host.contains(":")) {
            host = host.split(":")[0];
        }

        NetwardZone cached = hostCache.getIfPresent(host);
        if(cached != null) {
            logger.debug("Cache hit for host: {}, took {} ms", host, System.currentTimeMillis() - startTime);
            // Return null if it was cached as NOT_FOUND
            return cached == NOT_FOUND ? null : cached;
        }

        NetwardZone newZone = new NetwardZone();
        try(Connection sql =  App.dataSource.getConnection()) {
            var ps = sql.prepareStatement("SELECT * FROM `proxy_zones` WHERE `record` = ? LIMIT 1");     
            ps.setString(1, host);

            var rs = ps.executeQuery();
            if(rs.next()) {
                newZone.setId(rs.getInt("id"));
                newZone.setRecord(rs.getString("record"));
                newZone.setTarget(rs.getString("target"));

                hostCache.put(host, newZone);
                logger.debug("Fetched zone for host: {}, took {} ms", host, System.currentTimeMillis() - startTime);
                return newZone;
            } else {
                logger.warn("No zone found for host {}, in {}ms", host, System.currentTimeMillis() - startTime);
                // Cache the NOT_FOUND sentinel to avoid repeated DB queries
                hostCache.put(host, NOT_FOUND);
                return null;
            }

        } catch (SQLException e) {
            logger.error("Database error while fetching zone for host: " + host, e);
            return null;
        }
    }
    
}
