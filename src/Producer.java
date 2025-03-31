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
    
    public static int getInputs() {
        //Config values
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration file: " + e.getMessage());
            return -1;
        }

        // Retrieve the value from the config
        String c = config.getProperty("c");
        int NUM_CONSUMERS;

        if (c == null || c.isEmpty()) {
            System.out.println("Configuration property 'c' not found or is empty.");
            return -1;
        }

        try {
            NUM_CONSUMERS = Integer.parseInt(c);
            
            // Check if the value is negative
            if (NUM_CONSUMERS <= 0) {
                System.out.println("Number of consumers threads cannot be 0 or negative.");
                return -1;
            }    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format or out of integer bounds for c.");
            return -1;
        }

        return NUM_CONSUMERS;
    }
    
    public static void main(String[] args) {
        int NUM_PRODUCERS = getInputs();
        if (NUM_PRODUCERS == -1) {
            System.out.println("Failed to load configuration values.");
            return;
        }

        System.out.println("Producers: " + NUM_PRODUCERS);

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




