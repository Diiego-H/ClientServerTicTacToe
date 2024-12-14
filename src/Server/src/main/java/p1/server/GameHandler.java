package p1.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import p1.server.ServerProtocol.MessageType;
import utils.ComUtils;

public abstract class GameHandler implements Runnable {

    private static final int TIMEOUT = 2 * 60 * 1000;  // 2 minuts TimeOut
    private static final int MAX_ERR = 50;  // 50 errors màxim
    protected int errCounter;
    protected ServerEstat estat;
    private FileWriter logger;

    protected abstract void init() throws IOException;

    // Obtenció d'un fitxer on guardar els logs, per evitar sobreescriptures
    private File getFile(String session) {
        String name = session;
        File f = new File("logs/session" + name + ".log");
        if (f.isFile()) {
            // ID de sessió repetit, canviem el nom del fitxer
            int i = 0;
            do {
                i++;
                name = session + "(" + String.valueOf(i) + ")";
                f = new File("logs/session" + name + ".log");
            } while (f.isFile());
        }
        return f;
    }

    protected void createLogger(String s) throws IOException {
        File f = getFile(s);
        System.out.println("Session " + s + " messages will be logged in " + f.getName());
        try {
            logger = new FileWriter(f);
        } catch (IOException e) {
            throw new IOException("Problem opening the logger:\n" + e.getMessage());
        }
    }

    protected void log(String s) {
        try {
            logger.write(s);
        } catch (IOException e) {
            System.out.println("Problem while logging:\n" + e.getMessage());
        }
    }

    protected ServerProtocol createServerProtocol(Socket s) throws IOException {
        return new ServerProtocol(new ComUtils(s.getInputStream(), s.getOutputStream()));
    }

    // Gestió específica de la connexió a classes derivades
    protected abstract MessageType getMessageType() throws ServerException;
    protected abstract void setTimeout(int ms) throws ServerException;
    protected abstract void closeConnection();

    // Gestió específica de la lògica dels estats a classes derivades
    protected abstract void checkError(MessageType type) throws ServerException;
    protected abstract void helloLogic(MessageType type) throws ServerException;
    protected abstract void readyLogic(MessageType type) throws ServerException;
    protected abstract void playLogic(MessageType type) throws ServerException;

    @Override
    public void run() {
        // Imprimirem la data del log per poder ordenar els fitxers cronològicament
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        try {
            // Inicialització de la informació del GameHandler (millor fer-ho al nou thread i no al constructor)
            init();

            log("GAMEHANDLER STARTED (" + LocalDateTime.now().format(formatter) + ")\n");
            errCounter = 0;
            estat = ServerEstat.HELLO;
            setTimeout(TIMEOUT);

            // Maquina d'estats
            while (estat != ServerEstat.END) {
                try {
                    MessageType type = getMessageType();
                    switch(estat) {
                        case HELLO:
                            helloLogic(type);
                            break;
                        case READY:
                            readyLogic(type);
                            break;
                        case PLAY:
                            playLogic(type);
                            break;
                        case END:
                            break;
                    }
                } catch (ServerException e) {
                    log(e.getMessage() + "\n");
                    estat = ServerEstat.END;
                }

                if (errCounter == MAX_ERR) {
                    log(MAX_ERR + " errors have been sent to client. GameHandler will close to avoid an infinite execution\n");
                    estat = ServerEstat.END;
                }
            }

            log("Closing connection from server...\n");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ServerException e) {
            log(e.getMessage() + "\n");
        } finally {
            closeConnection();
            log("GAMEHANDLER FINISHED (" + LocalDateTime.now().format(formatter) + ")\n");
            try {
                logger.close();
            } catch (IOException e) {
                System.out.println("Problem closing the logger:\n" + e.getMessage());
            }
        }
    }
    
    protected enum ServerEstat {
        HELLO,  // Esperant HELLO de Client
        READY,  // Esperant PLAY de Client
        PLAY,   // Esperant ACTION de Client
        END,    // Desconnexio Client o tancament per error
    }
    
}