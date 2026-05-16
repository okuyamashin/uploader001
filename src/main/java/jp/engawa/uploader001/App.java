package jp.engawa.uploader001;

import io.javalin.Javalin;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.stream.Collectors;

public class App {

    public static void main(String[] args) throws Exception {
        Properties props = loadProperties("uploader001.properties");
        String uploadDir = props.getProperty("upload.dir");
        String baseUrl   = props.getProperty("base.url");
        int    port      = Integer.parseInt(props.getProperty("port"));

        Files.createDirectories(Paths.get(uploadDir));

        var app = Javalin.create().start(port);

        // 死活確認
        app.get("/health", ctx -> ctx.result("OK"));

        // 停止
        app.get("/stop", ctx -> {
            ctx.result("Stopping");
            new Thread(() -> {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                app.stop();
            }).start();
        });

        // アップロード: POST /upload
        app.post("/upload", ctx -> {
            byte[] body = ctx.bodyAsBytes();
            if (body.length == 0) {
                ctx.status(400).result("Empty body");
                return;
            }
            String ext = toExtension(ctx.contentType());
            String filename = md5Hex(body) + ext;
            Path dest = Paths.get(uploadDir, filename);
            Files.write(dest, body);
            ctx.result(baseUrl + filename);
        });

        // ダウンロード: GET /files/{filename}
        app.get("/files/{filename}", ctx -> {
            String name = ctx.pathParam("filename");
            if (name.contains("/") || name.contains("..")) {
                ctx.status(400).result("Invalid filename");
                return;
            }
            Path file = Paths.get(uploadDir, name);
            if (!Files.exists(file)) {
                ctx.status(404).result("Not found");
                return;
            }
            String mime = Files.probeContentType(file);
            ctx.contentType(mime != null ? mime : "application/octet-stream");
            ctx.result(Files.readAllBytes(file));
        });

        // ファイル一覧: GET /list
        app.get("/list", ctx -> {
            String list = Files.list(Paths.get(uploadDir))
                    .filter(Files::isRegularFile)
                    .map(p -> baseUrl + p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.joining("\n"));
            ctx.result(list);
        });
    }

    private static Properties loadProperties(String filename) throws IOException {
        var props = new Properties();
        try (var in = new FileInputStream(filename)) {
            props.load(in);
        }
        return props;
    }

    private static String toExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType.split(";")[0].trim().toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/gif"  -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp"  -> ".bmp";
            case "image/svg+xml" -> ".svg";
            default -> "";
        };
    }

    private static String md5Hex(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("MD5").digest(bytes);
        var sb = new StringBuilder(32);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
