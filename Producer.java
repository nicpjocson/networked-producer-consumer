import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;

public class Producer {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 12345;
   // private static final int BUFFER_SIZE = 1024;

    // Num of Producer threads, will add config later idk
    private static final int NUM_PRODUCERS = 2;

    int numProducers = Integer.parseInt(config.getProperty("p", "2"));
    int maxQueueLength = Integer.parseInt(config.getProperty("q", "10"));


    // idk change the name of the folders
    private static final String[] FOLDERS = {
        "folder1",
        "folder2",
        "folder3",
    };

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS);
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




