import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
public class Consumer {
    private final String saveDirectory;
    private final int numberOfThreads;
    private final int port;
    private boolean isRunning = true;
    private final BlockingQueue<Socket> videoQueue;
    private final Semaphore queueSemaphore = new Semaphore(1);
    
    public Consumer(int port, int numberOfThreads, int queueLength, String saveDirectory) {
        this.port = port;
        this.numberOfThreads = numberOfThreads;
        this.videoQueue = new LinkedBlockingQueue<>(queueLength);
        this.saveDirectory = saveDirectory;
        
        // Ensure save directory exists
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() {
        // Start c consumer worker threads
         ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads); // Create worker threads

        // Start worker threads to process the sockets from the queue
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(this::processSocket);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Consumer started on port " + port);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Remaining Capacity Queue: " + videoQueue.remainingCapacity());

                    if (videoQueue.remainingCapacity() == 0) {
                        // BONUS TODO: Notify Producer Queue is Full
                        System.out.println("Queue full, rejecting: " + clientSocket.getInetAddress());
                        clientSocket.close();
                    } else {
                        videoQueue.put(clientSocket);
                        System.out.println("Placed into Queue: " + clientSocket.getInetAddress());
                    }
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error handling socket: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }

    public void processSocket() {
        while (this.isRunning) {
            Socket socket = null;
            if (!videoQueue.isEmpty()) {
                try {
                    queueSemaphore.acquire();
                    // Take a video from queue
                    if (!videoQueue.isEmpty()) {
                        socket = videoQueue.take();
                    }
                    queueSemaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (socket != null) {
                    System.out.println("Processing: " + socket.getInetAddress());
                    try {
                        receiveAndSaveVideo(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void receiveAndSaveVideo(Socket socket) throws IOException {
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        // Read filename length and filename
        int fileNameLength = dis.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        dis.readFully(fileNameBytes);
        String fileName = new String(fileNameBytes);

        // Read file size
        long fileSize = dis.readLong();

        // Save to file
        File outputFile = new File(saveDirectory, fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            long remaining = fileSize;
            int bytesRead;

            while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
        }

        System.out.println("Video saved to: " + outputFile.getAbsolutePath());
        dis.close();
        socket.close();
    }

    public void stop() {
        this.isRunning = false;
        System.out.println("Stopping consumer...");
    }
}