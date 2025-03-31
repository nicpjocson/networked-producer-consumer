import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;

public class Producer {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;

    // Num of Producer threads, will add config later idk
    private static final int NUM_PRODUCERS = 2;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PRODUCERS);
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            executor.execute(new ProducerThread(HOST, PORT));
        }
        executor.shutdown();
    }

}


class ProducerThread implements Runnable {

    private String HOST;
    private int PORT;

    public ProducerThread(String host, int port) {
        this.HOST = host;
        this.PORT = port;
    }


}

