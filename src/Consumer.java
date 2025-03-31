import java.io.File;
public class Consumer {
    private final String saveDirectory;
    private final int numberOfThreads;
    private final int port;
    
    public Consumer(int port, int numberOfThreads, int queueLength, String saveDirectory) {
        this.port = port;
        this.numberOfThreads = numberOfThreads;
        // this.queue = new LinkedBlockingQueue<>(queueLength);
        this.saveDirectory = saveDirectory;
        
        // Ensure save directory exists
        File dir = new File(saveDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() {
        // Start consumer worker threads
    }
}