import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Consumer {

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
                System.out.println("Number of consumers cannot be 0 or negative.");
                return -1;
            }    
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format for 'c' or out of integer bounds: " + c);
            return -1;
        }

        return NUM_CONSUMERS;
    }

    public static void main (String[] args) {
        int c = getInputs();
        System.out.println(c);
    }
}