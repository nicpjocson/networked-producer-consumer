import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Set<String> knownHashes = ConcurrentHashMap.newKeySet();
    
    public Consumer(int port, int numberOfThreads, int queueLength, String saveDirectory) {
        this.port = port;
        this.numberOfThreads = numberOfThreads;
        this.socketQueue = new LinkedBlockingQueue<>(queueLength);
        this.saveDirectory = saveDirectory;
        
        // Ensure save directory exists
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
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
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                    String hash = dis.readUTF();

                    if (knownHashes.contains(hash)) {
                        dos.writeUTF("Duplicate Video!");
                        clientSocket.close();
                        continue;
                    } else {
                        dos.writeUTF("Unique Video");
                    }

                    queueSemaphore.acquire();
                    if (!socketQueue.add(clientSocket)) {
                        // Queue is full, reject the video and notify producer
                        System.out.println("Queue full, rejecting: " + clientSocket.getInetAddress());
                        dos.writeUTF("Video upload rejected: Queue full");
                        clientSocket.close();
                    } else {
                        // Successfully added to queue
                        System.out.println("Placed into Queue: " + clientSocket.getInetAddress());
                        System.out.println("Remaining Capacity Queue: " + socketQueue.remainingCapacity());
                    }
                    queueSemaphore.release();

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
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            int fileNameLength = dis.readInt();
            byte[] fileNameBytes = new byte[fileNameLength];
            dis.readFully(fileNameBytes);
            String fileName = new String(fileNameBytes);

            long fileSize = dis.readLong();
            File outputFile = new File(saveDirectory, fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[16384];
                long remaining = fileSize;
                int bytesRead;
                while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
            knownHashes.add(generateFileChecksum(outputFile));
            System.out.println("Saved: " + outputFile.getAbsolutePath());
        }
        socket.close();
    }

    private String generateFileChecksum(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Checksum error", e);
        }
    }

    public void stop() {
        this.isRunning = false;
        System.out.println("Stopping consumer...");
    }
}