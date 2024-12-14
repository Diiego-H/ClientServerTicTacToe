package p1.client;

import java.util.Random;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;

public class GameClientAI extends GameClientBase {

    private FileWriter logger;

    public GameClientAI(ClientProtocol protocol, Socket socket) {
        super(protocol, socket);
    }

    @Override
    protected String obtainPlayerName() {
        return "JohannesTheGreat444";
    }

    @Override
    protected String obtainPosition() {
        // Triem un moviment aleatori.
        Random random = new Random();
        int row;
        int col;
        do {
            row = random.nextInt(3);
            col = random.nextInt(3);
        } while (!board.cellIsEmpty(row, col));

        String randomRow = Integer.toString(row);
        String randomCol = Integer.toString(col);
        String position = randomRow + "-" + randomCol;
        handledLogMessage("The position chosen was: " + position);
        return position;
    }

    @Override
    protected boolean moveIsValid(String moviment) {
        int row = Character.getNumericValue(moviment.charAt(0));
        int col = Character.getNumericValue(moviment.charAt(2));

        return board.cellIsEmpty(row, col);
    }

    @Override
    protected boolean playAgain() {
        // El client automàtic tornarà a jugar aleatòriament, de moment un 75% dels cops
        // seguirem jugant.
        return Math.random() > 0.25;
    }

    // Obtenció d'un fitxer per evitar repeticions en els logs
    private File getUniqueFile(String filename) {
        String uniqueFilename = filename;
        File f = new File(uniqueFilename + ".log");
        if (f.isFile()) {
            // Nom de jugador repetit, canviem el nom del fitxer
            int i = 0;
            do {
                i++;
                uniqueFilename = filename + "(" + String.valueOf(i) + ")";
                f = new File(uniqueFilename + ".log");
            } while (f.isFile());
        }
        return f;
    }

    @Override
    protected void initLogger(String playerName) throws IOException {
        File f = getUniqueFile(playerName);
        logger = new FileWriter(f);
    }

    @Override
    protected void closeLogger() throws IOException {
        logger.close();
    }

    @Override
    protected void logMessage(String s) throws IOException {
        System.out.println(s);
        logger.write(s + "\n");
    }
}
