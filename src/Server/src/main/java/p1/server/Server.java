package p1.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import p1.server.ai.ServerAI;
import p1.server.proxy.ServerProxy;

public abstract class Server {
    public static final String INIT_ERROR = "Server should be initialized with -p <port>\n" +
                                            "Server may be initialized with -p <port> -m <mode>";
    public static final String MODE_ERROR = "<mode> should be 0 (ServerAI) or 1 (ServerProxy)";
    private static final String LOG_MSG = "Logs from matches will be saved in the directory logs";
    protected ServerSocket ss;
    private int port;

    protected Server(int port) {
        this.port = port;
        setConnection();
        new File("logs").mkdir();
        System.out.println(LOG_MSG);
    }

    private void setConnection() {
        if (this.ss == null) {
            try {
                ss = new ServerSocket(port);
                System.out.println("Server up & listening on port " + port + "...\nPress Cntrl + C to stop.");
            } catch (IOException e) {
                throw new RuntimeException("I/O error when opening the Server Socket:\n" + e.getMessage());
            }
        }
    }

    public abstract void init();

    public static void main(String[] args) {

        // Mode per defecte: ServerAI
        int mode = 0;

        if (args.length != 2 && args.length != 4) {
            throw new IllegalArgumentException("Wrong amount of arguments.\n" + INIT_ERROR);
        }

        if (!args[0].equals("-p")) {
            throw new IllegalArgumentException("Wrong argument keyword.\n" + INIT_ERROR);
        }

        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("<port> should be an Integer.");
        }

        if (args.length == 4) {
            if (!args[2].equals("-m")) {
                throw new IllegalArgumentException("Wrong argument keyword.\n" + INIT_ERROR);
            }

            NumberFormatException mode_error = new NumberFormatException(MODE_ERROR);

            try {
                mode = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                throw mode_error;
            }

            if (mode != 0 && mode != 1) {
                throw mode_error;
            }
        }

        // Creem un servidor en el mode escollit per terminal
        Server server;
        if (mode == 0) { server = new ServerAI(port); }
        else { server = new ServerProxy(port); }
        server.init();
    }
}
