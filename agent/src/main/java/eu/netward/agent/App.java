package eu.netward.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * EdgeProxy Agent - Certificate manager & active checker
 */
public class App {

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

        while(true) {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                logger.error("Main thread interrupted: {}", e.getMessage());
            }
        }

    }

}
