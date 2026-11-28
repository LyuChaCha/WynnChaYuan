package com.wynnchayuan.translate;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class RemoteSyncGzipTest {

    private static final byte[] ZIPPED = json("{\"entries\":{\"Hello\":{\"dst\":\"你好\"}}}");

    private static final byte[] PLAIN = json("{\"entries\":{\"Bye\":{\"dst\":\"再見\"}}}");

    private static final byte[] OLD = json("{\"entries\":{\"Old\":{\"dst\":\"舊的\"}}}");

    private static final List<String> asked = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("=== 譯文下載走 gzip ===");
        byte[] gz = gzip(ZIPPED);
        byte[] cut = Arrays.copyOf(gzip(PLAIN), gzip(PLAIN).length / 2);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serve(server, "/gz.json", gz, true);
        serve(server, "/plain.json", PLAIN, false);
        serve(server, "/cut.json", cut, true);
        serve(server, "/junk.json", "this is not gzip".getBytes(StandardCharsets.UTF_8), true);
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        Path dir = Files.createTempDirectory("wcy-gzip");
        try (HttpClient client = HttpClient.newHttpClient()) {
            Path a = dir.resolve("a.json");
            int got = RemoteSync.fetchFile(client, List.of(base + "/gz.json"), a);
            check("★ 壓縮過的檔：存下來的是解壓後的原文", got == RemoteSync.CHANGED
                    && Arrays.equals(Files.readAllBytes(a), ZIPPED));
            check("★ 請求帶 Accept-Encoding: gzip", asked.contains("gzip"));
            check("★ 同一份再抓一次：拿解壓後的內容比對，算沒變",
                    RemoteSync.fetchFile(client, List.of(base + "/gz.json"), a) == RemoteSync.SAME);

            Path b = dir.resolve("b.json");
            check("沒壓縮的檔照舊存下來",
                    RemoteSync.fetchFile(client, List.of(base + "/plain.json"), b) == RemoteSync.CHANGED
                            && Arrays.equals(Files.readAllBytes(b), PLAIN));

            Path c = dir.resolve("c.json");
            check("★ 壓縮資料壞掉：算失敗",
                    RemoteSync.fetchFile(client, List.of(base + "/cut.json"), c) == RemoteSync.FAILED);
            check("★ 壓縮資料壞掉：不留半截的檔", !Files.exists(c)
                    && !Files.exists(dir.resolve("c.json.tmp")));
            check("聲稱 gzip 卻不是 gzip：算失敗、不留檔",
                    RemoteSync.fetchFile(client, List.of(base + "/junk.json"), c) == RemoteSync.FAILED
                            && !Files.exists(c));

            Files.write(c, OLD);
            check("★ 壓縮資料壞掉：原本的快取原封不動",
                    RemoteSync.fetchFile(client, List.of(base + "/cut.json"), c) == RemoteSync.FAILED
                            && Arrays.equals(Files.readAllBytes(c), OLD));
            check("第一個來源壞掉就換下一個",
                    RemoteSync.fetchFile(client, List.of(base + "/cut.json", base + "/gz.json"), c)
                            == RemoteSync.CHANGED && Arrays.equals(Files.readAllBytes(c), ZIPPED));
        } finally {
            server.stop(0);
            deleteTree(dir);
        }
        System.out.println(failures == 0
                ? "RemoteSyncGzip：全部通過" : "RemoteSyncGzip：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void serve(HttpServer server, String path, byte[] body, boolean zipped) {
        server.createContext(path, exchange -> {
            String accept = exchange.getRequestHeaders().getFirst("Accept-Encoding");
            synchronized (asked) {
                asked.add(accept == null ? "" : accept);
            }
            if (zipped) {
                exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            }
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    private static byte[] json(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] gzip(byte[] raw) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(raw);
        }
        return bytes.toByteArray();
    }

    private static void deleteTree(Path root) throws Exception {
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
