package dev.lucas.clickutils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Talks to CheatUtils local HTTP API (127.0.0.1:5005+). */
final class CheatUtilsApi {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(350))
            .build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "GayBoi5000-API");
        t.setDaemon(true);
        return t;
    });

    private static volatile int port = 5005;

    private CheatUtilsApi() {}

    static int port() {
        return port;
    }

    static void loadModules(Consumer<JsonObject> ok, Consumer<String> err) {
        WORKER.execute(() -> {
            String last = "CheatUtils not found on ports 5005-5104";
            for (int p = 5005; p <= 5104; p++) {
                try {
                    JsonObject body = request(p, "GET", "/api/modules-status", null);
                    port = p;
                    Minecraft.getInstance().execute(() -> ok.accept(body));
                    return;
                } catch (Exception e) {
                    last = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                }
            }
            String msg = last;
            Minecraft.getInstance().execute(() -> err.accept(msg));
        });
    }

    static void toggle(String path, boolean enabled, Consumer<JsonObject> ok, Consumer<String> err) {
        WORKER.execute(() -> {
            try {
                String apiPath = path.startsWith("/") ? path : "/api/" + path;
                JsonObject cfg = request(port, "GET", apiPath, null);
                cfg.addProperty("enabled", enabled);
                JsonObject out = request(port, "POST", apiPath, GSON.toJson(cfg));
                Minecraft.getInstance().execute(() -> ok.accept(out));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Minecraft.getInstance().execute(() -> err.accept(msg));
            }
        });
    }

    static void getConfig(String path, Consumer<JsonObject> ok, Consumer<String> err) {
        WORKER.execute(() -> {
            try {
                String apiPath = path.startsWith("/") ? path : "/api/" + path;
                JsonObject cfg = request(port, "GET", apiPath, null);
                Minecraft.getInstance().execute(() -> ok.accept(cfg));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Minecraft.getInstance().execute(() -> err.accept(msg));
            }
        });
    }

    static void postConfig(String path, JsonObject cfg, Consumer<JsonObject> ok, Consumer<String> err) {
        WORKER.execute(() -> {
            try {
                String apiPath = path.startsWith("/") ? path : "/api/" + path;
                JsonObject out = request(port, "POST", apiPath, GSON.toJson(cfg));
                Minecraft.getInstance().execute(() -> ok.accept(out));
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Minecraft.getInstance().execute(() -> err.accept(msg));
            }
        });
    }

    private static JsonObject request(int p, String method, String path, String body) throws Exception {
        String url = "http://127.0.0.1:" + p + path;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(900));
        if ("GET".equals(method)) {
            b.GET();
        } else {
            b.header("Content-Type", "application/json; charset=utf-8");
            b.POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
        }
        HttpResponse<String> res = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + res.statusCode() + ": " + res.body());
        }
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }
}
