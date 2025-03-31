import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class Consumer {
    public static int[] getInputs() {
        // Config values
        Properties config = new Properties();
        
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        } catch (IOException e) {
            System.out.println("Failed to load configuration file: " + e.getMessage());
            return null; // Returning null to indicate failure
        }
    
        String c = config.getProperty("c");
        String q = config.getProperty("q");
    
        if (c == null || c.isEmpty()) {
            System.out.println("Configuration property 'c' not found or is empty.");
            return null;
        }
    
        if (q == null || q.isEmpty()) {
            System.out.println("Configuration property 'q' not found or is empty.");
            return null;
        }
    
        int NUM_CONSUMERS, QUEUE_LENGTH;
    
        try {
            NUM_CONSUMERS = Integer.parseInt(c);
            QUEUE_LENGTH = Integer.parseInt(q);
    
            // Validate values
            if (NUM_CONSUMERS <= 0) {
                System.out.println("Number of consumer threads cannot be 0 or negative.");
                return null;
            }
    
            if (QUEUE_LENGTH <= 0) {
                System.out.println("Queue length cannot be 0 or negative.");
                return null;
            }    
    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format or integer out of bounds for 'c' or 'q'.");
            return null;
        }
    
        return new int[]{NUM_CONSUMERS, QUEUE_LENGTH};
    }
    
    public static void main(String[] args) {
        int[] inputs = getInputs();
        if (inputs == null) {
            System.out.println("Failed to load configuration values.");
            return;
        }

        int NUM_CONSUMERS = inputs[0];
        int QUEUE_LENGTH = inputs[1];
        System.out.println("Consumers: " + NUM_CONSUMERS + ", Queue Length: " + QUEUE_LENGTH);
    }
}