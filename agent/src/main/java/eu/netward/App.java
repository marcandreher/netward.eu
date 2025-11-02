package eu.netward;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * EdgeProxy Agent - Reverse Proxy
 */
public class App extends AbstractVerticle {
    
    private HttpClient client;
    private static final int PORT = 8080;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new App())
            .onSuccess(id -> System.out.println("✓ Verticle deployed successfully: " + id))
            .onFailure(err -> {
                System.err.println("✗ Failed to deploy verticle: " + err.getMessage());
                err.printStackTrace();
            });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        // Create HTTP client
        client = vertx.createHttpClient(new HttpClientOptions()
                .setKeepAlive(true)
                .setIdleTimeout(60)
        );

        // Create HTTP server
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(req -> handleProxy(req, client));

        // Start the server
        server.listen(PORT)
            .onSuccess(s -> {
                System.out.println("⚡ Proxy server started on port " + PORT);
                startPromise.complete();
            })
            .onFailure(err -> {
                System.err.println("✗ Failed to start server: " + err.getMessage());
                startPromise.fail(err);
            });
    }

    private void handleProxy(HttpServerRequest req, HttpClient client) {
        // Example: You could resolve origin dynamically based on hostname
        // For now, forward everything to localhost:8082
        String targetHost = "localhost";
        int targetPort = 555;

        client.request(req.method(), targetPort, targetHost, req.uri())
            .compose(proxyReq -> {
                proxyReq.headers().setAll(req.headers());
                return proxyReq.send(req);
            })
            .onSuccess(proxyRes -> {
                HttpServerResponse clientRes = req.response();
                clientRes.setStatusCode(proxyRes.statusCode());
                clientRes.headers().setAll(proxyRes.headers());
                proxyRes.body()
                    .onSuccess(body -> clientRes.end(body))
                    .onFailure(err -> {
                        clientRes.setStatusCode(502).end("Bad Gateway: " + err.getMessage());
                    });
            })
            .onFailure(err -> {
                req.response().setStatusCode(502).end("Proxy Error: " + err.getMessage());
            });
    }

    
}
