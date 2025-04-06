import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class ConsumerApp {
    public static int[] getInputs() {
        // Config values
        Properties config = new Properties();
        
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration file: " + e.getMessage());
            return null; // Returning null to indicate failure
        }
    
        String c = config.getProperty("c");
        String q = config.getProperty("q");
    
        if (c == null || c.isEmpty()) {
            System.out.println("Configuration property 'c' not found or is empty.");
            return null;
        }
    
        if (q == null || q.isEmpty()) {
            System.out.println("Configuration property 'q' not found or is empty.");
            return null;
        }
    
        int NUM_CONSUMERS, QUEUE_LENGTH;
    
        try {
            NUM_CONSUMERS = Integer.parseInt(c);
            QUEUE_LENGTH = Integer.parseInt(q);
    
            // Validate values
            if (NUM_CONSUMERS <= 0) {
                System.out.println("Number of consumer threads cannot be 0 or negative.");
                return null;
            }
    
            if (QUEUE_LENGTH <= 0) {
                System.out.println("Queue length cannot be 0 or negative.");
                return null;
            }    
    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format or integer out of bounds for 'c' or 'q'.");
            return null;
        }
    
        return new int[]{NUM_CONSUMERS, QUEUE_LENGTH};
    }

    public static List<String> getVideoFiles(String folderPath) {
        File folder = new File(folderPath);
        List<String> videoFiles = new ArrayList<>();
        
        // List all .mp4 files in the given folder
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".mp4"));
            if (files != null) {
                for (File file : files) {
                    videoFiles.add(file.getName()); // Add filename to list
                }
            }
        } else {
            System.out.println("The folder " + folderPath + " does not exist or is not a directory.");
        }
        
        return videoFiles;
    }
    
    public static void main(String[] args) {
        int[] inputs = getInputs();
        if (inputs == null) {
            System.out.println("Failed to load configuration values.");
            return;
        }

        int NUM_CONSUMERS = inputs[0];
        int QUEUE_LENGTH = inputs[1];
        System.out.println("Consumers: " + NUM_CONSUMERS + ", Queue Length: " + QUEUE_LENGTH);

        String saveDirectory = "consumer_videos";
        List<String> videoFiles = getVideoFiles(saveDirectory);
        
        if (videoFiles.isEmpty()) {
            System.out.println("No video files found in the 'consumer_videos' folder.");
        } else {
            System.out.println("Video files found:");
            for (String video : videoFiles) {
                System.out.println(video);
            }
        }

        try {
            // Start HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
            System.out.println("Starting HTTP server on http://localhost:8080");

            // Serve index.html at /index.html
            server.createContext("/index.html", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    // File file = new File("src/index.html"); // Direct reference to the index.html file
                    File file = new File(System.getProperty("user.dir"), "src/index.html");
                    
                    if (file.exists()) {
                        // Read file content
                        FileInputStream fis = new FileInputStream(file);
                        byte[] fileContent = fis.readAllBytes();
                        fis.close();
                        
                        // Send response headers (200 OK, content length, content type)
                        exchange.getResponseHeaders().add("Content-Type", "text/html");
                        exchange.sendResponseHeaders(200, fileContent.length);
                        
                        // Write content to response body
                        OutputStream os = exchange.getResponseBody();
                        os.write(fileContent);
                        os.close();
                    } else {
                        // If file doesn't exist, send 404 response
                        String response = "404 Not Found: index.html";
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                }
            });            
            
            // Start the server
            server.start();
        } catch (IOException e) {
            System.out.println("Error starting HTTP server: " + e.getMessage());
        }

        int consumerPort = 12345; // Consumer listens on this port
        Consumer consumer = new Consumer(consumerPort, NUM_CONSUMERS, QUEUE_LENGTH, saveDirectory);
        consumer.start();
    }
}