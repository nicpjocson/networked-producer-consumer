import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class ConsumerApp {
    private static HttpServer server;
    private static Consumer consumer;

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

    public static void stopApplication() {
        System.out.println("Shutting down...");
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP server stopped.");
        }
        if (consumer != null) {
            consumer.stop();
            System.out.println("Consumer stopped.");
        }
        System.out.println("Exiting program.");
        System.exit(0);
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

        int httpPort = 8081;
        int consumerPort = 12345; // Consumer listens on this port

        try {
            // Create HTTP server on localhost at port 8081
            server = HttpServer.create(new InetSocketAddress(httpPort), 0);

            server.createContext("/index.html", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    File file = new File("src/index.html");
            
                    if (file.exists()) {
                        FileInputStream fis = new FileInputStream(file);
                        byte[] fileContent = fis.readAllBytes();
                        fis.close();
            
                        // Modify HTML content
                        String filenames = "";
                        for (String video : videoFiles) {
                            if (!filenames.isEmpty()) filenames += ",";
                            filenames += URLEncoder.encode(video, "UTF-8");
                        }
            
                        // Replace placeholders with actual values
                        String content = new String(fileContent);
                        content = content.replace("{{video_filenames}}", filenames);
                        content = content.replace("{{save_directory}}", saveDirectory);
            
                        // Send modified HTML back
                        exchange.getResponseHeaders().add("Content-Type", "text/html");
                        exchange.sendResponseHeaders(200, content.getBytes().length);
            
                        OutputStream os = exchange.getResponseBody();
                        os.write(content.getBytes());
                        os.close();
                    } else {
                        String response = "404 Not Found: index.html";
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    }
                }
            });            

            // Serve video files at /consumer_videos/filename
            server.createContext("/consumer_videos", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    // Extract video filename from request URI
                    String path = exchange.getRequestURI().getPath();
                    // Decode URL-encoded filename to handle spaces and special characters
                    String filename = URLDecoder.decode(path.substring("/consumer_videos/".length()), "UTF-8");

                    // Set video's directory
                    File videoFile = new File(saveDirectory + "/" + filename);
            
                    if (!videoFile.exists() || !videoFile.isFile()) {
                        String response = "404 Not Found: Video " + filename;
                        exchange.sendResponseHeaders(404, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
            
                    String range = exchange.getRequestHeaders().getFirst("Range");
                    long fileLength = videoFile.length();
                    long start = 0;
                    long end = fileLength - 1;
            
                    if (range != null && range.startsWith("bytes=")) {
                        String[] parts = range.substring(6).split("-");
                        try {
                            start = Long.parseLong(parts[0]);
                            if (parts.length > 1 && !parts[1].isEmpty()) {
                                end = Long.parseLong(parts[1]);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
            
                    long contentLength = end - start + 1;
                    String contentRange = "bytes " + start + "-" + end + "/" + fileLength;
            
                    exchange.getResponseHeaders().add("Content-Type", "video/mp4");
                    exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
                    exchange.getResponseHeaders().add("Content-Range", contentRange);
                    exchange.sendResponseHeaders(206, contentLength); // 206 Partial Content
            
                    try (OutputStream os = exchange.getResponseBody();
                         FileInputStream fis = new FileInputStream(videoFile)) {
            
                        fis.skip(start);
                        byte[] buffer = new byte[8192];
                        long toSend = contentLength;
                        int len;
            
                        while (toSend > 0 && (len = fis.read(buffer, 0, (int)Math.min(buffer.length, toSend))) != -1) {
                            os.write(buffer, 0, len);
                            toSend -= len;
                        }
                    }
                }
            });

            server.createContext("/api/videos", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    List<String> currentVideos = getVideoFiles(saveDirectory);
            
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < currentVideos.size(); i++) {
                        json.append("\"").append(URLEncoder.encode(currentVideos.get(i), "UTF-8")).append("\"");
                        if (i < currentVideos.size() - 1) json.append(",");
                    }
                    json.append("]");
            
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, json.toString().getBytes().length);
            
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(json.toString().getBytes());
                    }
                }
            });            

            // Start server
            server.start();
            System.out.println("Server started at http://localhost:" + httpPort);

            consumer = new Consumer(consumerPort, NUM_CONSUMERS, QUEUE_LENGTH, saveDirectory);
            consumer.start();

            Thread exitThread = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.println("Type 'exit' to stop the server and exit the program.");
                    String input = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(input)) {
                        stopApplication();
                    }
                }
            });
            exitThread.setDaemon(true);
            exitThread.start();

        } catch (IOException e) {
            System.out.println("Error starting HTTP server: " + e.getMessage());
        }
    }
}