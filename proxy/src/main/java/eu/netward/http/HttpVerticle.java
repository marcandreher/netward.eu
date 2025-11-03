package eu.netward.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.netward.proxy.ProxyHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.PoolOptions;

public class HttpVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

    private HttpClient client;

    private ProxyHandler proxyHandler;
    private int port = 8080;

    public HttpVerticle(ProxyHandler proxyHandler, int port) {
        this.proxyHandler = proxyHandler;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        // Create HTTP client with better settings for proxying
        Integer http1PoolSize = Integer.parseInt(System.getenv().getOrDefault("NETWARD_HTTP1_POOL", "20"));

        PoolOptions options = new PoolOptions().setHttp1MaxSize(http1PoolSize);

        client = vertx
                .httpClientBuilder()
                .with(options)
                .with(new HttpClientOptions()
                        .setKeepAlive(true)
                        .setIdleTimeout(120)
                        .setConnectTimeout(10000))
                .build();

        // Create HTTP server
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(req -> {
            // Pause the request immediately to prevent it from being consumed
            req.pause();
            proxyHandler.handleProxy(req, client);
        });

        // Start the server
        server.listen(port)
                .onSuccess(s -> {
                    logger.info("⚡ Proxy server started on port " + port);
                    startPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("✗ Failed to start server: " + err.getMessage(), err);
                    startPromise.fail(err);
                });
    }

}
