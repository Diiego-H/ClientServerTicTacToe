package p1.client;

import java.util.Scanner;
import java.net.Socket;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GameClient extends GameClientBase {

    private Scanner scanner = null;
    private FileWriter logger;

    public GameClient(ClientProtocol protocol, Socket socket) {
        super(protocol, socket);
    }

    private Scanner getScanner() {
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        return scanner;
    }

    @Override
    protected String obtainPlayerName() {
        // Li demanem a l'usuari el seu nom de jugador.
        Scanner scanner = getScanner();
        System.out.println("What is your player name?");
        String playerName = scanner.nextLine();
        return playerName;
    }

    @Override
    protected String obtainPosition() {
        // Li demanem a l'usuari quin moviment vol fer.
        Scanner scanner = getScanner();
        handledLogMessage("Your move (Eg: 0-2, row-column): ");
        String position = scanner.nextLine();
        while (position.length() != 3) {
            handledLogMessage(
                    "The move provided is not the right length! It should have the shape row-column, "
                            + "where row and column are numbers between 0 and 2. Eg: 1-1, 2-1, 0-1,... Try again: ");
            position = scanner.nextLine();
        }
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
        // Li demanem a l'usuari si vol tornar a jugar
        Scanner scanner = getScanner();
        handledLogMessage("Do you want to play again? (Y for yes / N for no)");
        String position = scanner.nextLine().toUpperCase();
        while (!position.equals("Y") && !position.equals("N")) {
            handledLogMessage("You must only write a Y or an N:");
            position = scanner.nextLine().toUpperCase();
        }

        if (position.equals("Y")) {
            return true;
        } else {
            scanner.close();
            return false;
        }
    }

    // Obtenció d'un fitxer per evitar repeticions en els logs
    private File getUniqueFile(String filename) {
        String uniqueFilename = filename;
        File f = new File(uniqueFilename + ".log");
        if (f.isFile()) {
            // Nom d'usuari repetit, canviem el nom del fitxer
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
