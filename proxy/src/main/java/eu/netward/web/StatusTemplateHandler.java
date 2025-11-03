package eu.netward.web;

import eu.netward.model.HttpStatus;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.vertx.core.http.HttpServerRequest;

public class StatusTemplateHandler {

    public static void handle(TemplateEngine templateEngine, HttpServerRequest req, HttpStatus status, String message, String requestId) {
         var output = new StringOutput();
            var map = new java.util.HashMap<String, Object>();
            map.put("host", req.getHeader("host") != null ? req.getHeader("host") : "unknown");
            map.put("message", message);
            map.put("requestId", requestId);
            map.put("status", status.getCode());
            map.put("statusFull", status.getReasonPhrase());
            map.put("ipAddress", req.remoteAddress().toString());

            templateEngine.render("status.jte", map, output);

            req.response()
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .setStatusCode(status.getCode())
                .end(output.toString());
    }
    
}
