package eu.netward;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import eu.netward.http.HttpVerticle;
import eu.netward.proxy.ProxyHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

/**
 * EdgeProxy Agent - Reverse Proxy
 */
public class App extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static HikariDataSource dataSource;

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + System.getenv().getOrDefault("MYSQL_HOST", "localhost") + ":" + System.getenv().getOrDefault("MYSQL_PORT", "3306") + "/"  + System.getenv().getOrDefault("MYSQL_DATABASE", "netward"));
        config.setUsername(System.getenv().getOrDefault("MYSQL_USER", "netward"));
        config.setPassword(System.getenv().getOrDefault("MYSQL_PASSWORD", "password"));
        
        // Optional tuning
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        logger.info("✓ Database connection pool initialized in {} ms.", (System.currentTimeMillis() - startTime));

        Vertx vertx = Vertx.vertx();

        ProxyHandler proxyHandler = new ProxyHandler();


        vertx.deployVerticle(new HttpVerticle(proxyHandler, 8080))
                .onSuccess(id -> logger.info("✓ Verticle deployed successfully: ID: {}", id))
                .onFailure(err -> {
                    logger.error("✗ Failed to deploy verticle: {}", err.getMessage(), err);
                });

        long endTime = System.currentTimeMillis();
        logger.info("⚡ NetWard Proxy started in {} ms.", (endTime - startTime));
    }

}
