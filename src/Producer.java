import java.io.*;
import java.net.Socket;
import java.util.Properties;
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
    
    public static int[] getInputs() {
        // Config values
        Properties config = new Properties();
        
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration file: " + e.getMessage());
            return null; // Returning null to indicate failure
        }
    
        String p = config.getProperty("p");
        String q = config.getProperty("q");
    
        if (p == null || p.isEmpty()) {
            System.out.println("Configuration property 'p' not found or is empty.");
            return null;
        }
    
        if (q == null || q.isEmpty()) {
            System.out.println("Configuration property 'q' not found or is empty.");
            return null;
        }
    
        int NUM_PRODUCERS, QUEUE_LENGTH;
    
        try {
            NUM_PRODUCERS = Integer.parseInt(p);
            QUEUE_LENGTH = Integer.parseInt(q);
    
            // Validate values
            if (NUM_PRODUCERS <= 0) {
                System.out.println("Number of producers cannot be 0 or negative.");
                return null;
            }
    
            if (QUEUE_LENGTH <= 0) {
                System.out.println("Queue length cannot be 0 or negative.");
                return null;
            }    
    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format or integer out of bounds for 'p' or 'q'.");
            return null;
        }
    
        return new int[]{NUM_PRODUCERS, QUEUE_LENGTH};
    }
    
    public static void main(String[] args) {
        int[] inputs = getInputs();
        if (inputs == null) {
            System.out.println("Failed to load configuration values.");
            return;
        }

        int NUM_PRODUCERS = inputs[0];
        int QUEUE_LENGTH = inputs[1];
        System.out.println("Producers: " + NUM_PRODUCERS + ", Queue Length: " + QUEUE_LENGTH);

        //making da thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS);
        
        // assinging the folgers to the p threads
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            executor.execute(new ProducerThread(HOST, PORT, FOLDERS[i]));
        }
        executor.shutdown();
    }

}

class ProducerThread implements Runnable {

    private String HOST;
    private int PORT;
    private String FOLDER;

    public ProducerThread(String HOST, int PORT, String FOLDER) {
        this.HOST = HOST;
        this.PORT = PORT;
        this.FOLDER = FOLDER;
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

        for (File file : files){
            sendToConsumer(file);
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


            byte[] buffer = new byte[1024];
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




