import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Producer {
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
    
    public static void main(String[] args) {
        String consumerHost = "localhost";
        int consumerPort = 12345;
        int NUM_PRODUCERS = getInputsP();
        if (NUM_PRODUCERS == -1) {
            System.out.println("Failed to load configuration values.");
            return;
        }
        File root = new File("producer_folders");
        File[] folders = root.listFiles(File::isDirectory);
        if (folders == null || folders.length < NUM_PRODUCERS) {
            System.out.println("Not enough folders for producer threads.");
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS);
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            if (folders[i] == null) {
                continue;
            } else {
                executor.execute(new ProducerThread(consumerHost, consumerPort, folders[i].getAbsolutePath()));
            }
        }
        executor.shutdown();
    }
}

class ProducerThread implements Runnable {
    private String consumerHost;
    private int consumerPort;
    private String folder;

    public ProducerThread(String consumerHost, int consumerPort, String folder) {
        this.consumerHost = consumerHost;
        this.consumerPort = consumerPort;
        this.folder = folder;
    }

    @Override
    public void run() {
        sendVideosFromFolder();
    }

    private void sendVideosFromFolder() {
        File folder = new File(this.folder);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".mp4"));
        if (files == null) {
            System.err.println("Invalid folder or no videos: " + folder);
            return;
        }

        for (File file : files) {
            try {
                sendVideo(file);
                // Thread.sleep(1000); // Simulate upload interval
            } catch (IOException e) {
                // System.err.println("Error sending video: " + file.getName() + " -> " + e.getMessage());
            }
        }
    }

    private void sendVideo(File videoFile) throws IOException {
        String hash = computeHash(videoFile);
        try (Socket socket = new Socket(this.consumerHost, this.consumerPort);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF(hash);
            String response = dis.readUTF();

            if ("Duplicate Video!".equals(response)) {
                System.out.println("Duplicate: " + videoFile.getName());
                return;
            } else if ("Video upload rejected: Queue full".equals(response)) {
                System.out.println("Queue full: " + videoFile.getName());
                return;
            }

            byte[] nameBytes = videoFile.getName().getBytes();
            dos.writeInt(nameBytes.length);
            dos.write(nameBytes);

            dos.writeLong(videoFile.length());
            try (FileInputStream fis = new FileInputStream(videoFile)) {
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("Uploaded: " + videoFile.getName());
        }
    }

    private String computeHash(File file) throws IOException {
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
            throw new IOException("Hash error", e);
        }
    }
}




