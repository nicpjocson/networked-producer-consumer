import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.nio.file.Files;
import java.net.InetSocketAddress;

public class MyHttpServer {
    public static void main(String[] args) throws IOException {
        int port = 8000;
        String rootDir = "src/public";  // Corrected to match your folder structure

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new StaticFileHandler(rootDir));
        server.setExecutor(null);
        server.start();

        System.out.println("Server started at http://localhost:" + port);
    }
}


class StaticFileHandler implements HttpHandler {
    private final String rootDir;

    public StaticFileHandler(String rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String filePath = rootDir + exchange.getRequestURI().getPath();
        File file = new File(filePath);

        if (file.exists() && !file.isDirectory()) {
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
        } else {
            String response = "404 Not Found";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
