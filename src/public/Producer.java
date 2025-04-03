import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Producer {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 12345;
   // private static final int BUFFER_SIZE = 1024;

    // idk change the name of the folders
    private static final String[] FOLDERS = {
        "folder1",
        "folder2",
        "folder3",
    };
    
    
    public static int getInputsP() {
        //Config values
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration file: " + e.getMessage());
            return -1;
        }

        // Retrieve the value from the config
        String p = config.getProperty("p");
        int NUM_PRODUCERS;

        if (p == null || p.isEmpty()) {
            System.out.println("Configuration property 'c' not found or is empty.");
            return -1;
        }

        try {
            NUM_PRODUCERS = Integer.parseInt(p);
            
            // Check if the value is negative
            if (NUM_PRODUCERS <= 0) {
                System.out.println("Number of producers threads cannot be 0 or negative.");
                return -1;
            }    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format or out of integer bounds for c.");
            return -1;
        }

        return NUM_PRODUCERS;
    }

    public static int getInputsQ() {
        //Config values
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration file: " + e.getMessage());
            return -1;
        }

        // Retrieve the value from the config
        String q = config.getProperty("q");
        int QUEUE_LENGTH;

        if (q == null || q.isEmpty()) {
            System.out.println("Configuration property 'q' not found or is empty.");
            return -1;
        }

        try {
            QUEUE_LENGTH = Integer.parseInt(q);
            
            // Check if the value is negative
            if (QUEUE_LENGTH <= 0) {
                System.out.println("Queue Length cannot be 0 or negative.");
                return -1;
            }    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format or out of integer bounds for q.");
            return -1;
        }

        return QUEUE_LENGTH;
    }
    
    public static void main(String[] args) {
        int NUM_PRODUCERS = getInputsP();
        int QUEUE_LENGTH = getInputsQ();
        if (NUM_PRODUCERS == -1) {
            System.out.println("Failed to load configuration values.");
            return;
        }

        if (QUEUE_LENGTH == -1) {
            System.out.println("Failed to load configuration values.");
            return;
        }

        System.out.println("Queue Length: " + QUEUE_LENGTH);

        System.out.println("Producers: " + NUM_PRODUCERS);

        BlockingQueue<File> QUEUE = new ArrayBlockingQueue<>(QUEUE_LENGTH);


        //making da thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS);
        
        // assinging the folgers to the p threads
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            String assignedFolder = FOLDERS[i % FOLDERS.length]; 
            executor.execute(new ProducerThread(HOST, PORT, assignedFolder, QUEUE));
        }
        executor.shutdown();
    }

}

class ProducerThread implements Runnable {

    private String HOST;
    private int PORT;
    private String FOLDER;
    private final BlockingQueue<File> QUEUE;

    public ProducerThread(String HOST, int PORT, String FOLDER, BlockingQueue<File> QUEUE) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.FOLDER = FOLDER;
        this.QUEUE = QUEUE;
    }

    @Override
    public void run() {
        File chosenFolder = new File(FOLDER);

        if (!chosenFolder.exists()) {
            System.out.println("Folder does not exist");
            return;
        }

        File[] files = chosenFolder.listFiles((dir, name) -> name.endsWith(".mp4"));
        if (files == null) {
            System.out.println("No files found in folder");
            return;
        }

        for (File file : files) {
            try {
                if (!QUEUE.offer(file)) {
                    System.out.println("Queue is full. Dropping file: " + file.getName());
                } else {
                    System.out.println("Added file to queue: " + file.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        while (!QUEUE.isEmpty()) {
            try {
                File file = QUEUE.take();
                sendToConsumer(file);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    private void sendToConsumer (File file){
        try (Socket socket = new Socket(HOST, PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {

            dos.writeUTF(file.getName());
            dos.flush();

            long fileSize = file.length();
            dos.writeLong(file.length());
            dos.flush();


            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            dos.flush();

            System.out.println("File " + file.getName() + " sent to consumer");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




