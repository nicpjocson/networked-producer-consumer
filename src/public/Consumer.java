import java.io.File;
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

        while (this.isRunning) { 
            // Waiting for sockets

            // Check if queue is full, if full
                // Notify its full
                // Don't accept

            // Put video into queue if it is not full
        }
    }

    public void processSocket() {
        while (this.isRunning) {
            Socket socket;
            try {
                queueSemaphore.acquire();
                // Take a video from queue
                socket = videoQueue.take();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Processing video: ");
            receiveAndSaveVideo();
        }
    }

    public void receiveAndSaveVideo() {

    }
}