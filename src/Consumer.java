import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private final BlockingQueue<Socket> socketQueue;
    private final Semaphore queueSemaphore = new Semaphore(1);
    
    public Consumer(int port, int numberOfThreads, int queueLength, String saveDirectory) {
        this.port = port;
        this.numberOfThreads = numberOfThreads;
        this.socketQueue = new LinkedBlockingQueue<>(queueLength);
        this.saveDirectory = saveDirectory;
        
        // Ensure save directory exists
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() {
        // Start c consumer worker threads
         ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads + 1); // Create worker threads

        // Start worker threads to process the sockets from the queue
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(this::processSocket);
        }

        // Create a separate thread for accepting and handling socket connections
        executorService.submit(this::acceptAndHandleConnections);
        executorService.shutdown();
    }

    public void acceptAndHandleConnections() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Consumer started on port " + port);

            while (isRunning) {
                try {
                    // Accept client connections
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Received connection from: " + clientSocket.getInetAddress());

                    if (socketQueue.remainingCapacity() == 0) {
                        // BONUS TODO: Notify Producer Queue is Full
                        System.out.println("Queue full, rejecting: " + clientSocket.getInetAddress());
                        // Send rejection message to the producer before closing the socket
                        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
                            dos.writeUTF("Video upload rejected: Queue full");
                        }
                        clientSocket.close();
                    } else {
                        queueSemaphore.acquire();
                        socketQueue.put(clientSocket);
                        queueSemaphore.release();
                        System.out.println("Placed into Queue: " + clientSocket.getInetAddress());
                        System.out.println("Remaining Capacity Queue: " + socketQueue.remainingCapacity());
                    }

                } catch (IOException | InterruptedException e) {
                    System.err.println("Error handling socket: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    public void processSocket() {
        while (this.isRunning) {
            Socket socket = null;
            if (!socketQueue.isEmpty()) {
                try {
                    queueSemaphore.acquire();
                    // Take a video from queue
                    if (!socketQueue.isEmpty()) {
                        socket = socketQueue.take();
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

        System.out.println("Received video: " + fileName);

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
        socket.close(); socket.close(); // TODO: Do I close this?
    }

    public void stop() {
        this.isRunning = false;
        System.out.println("Stopping consumer...");
    }
}