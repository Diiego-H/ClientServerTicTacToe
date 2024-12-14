package p1.client;

import java.util.Random;
import java.net.Socket;

public class GameClientTest extends GameClientBase {

    public GameClientTest(ClientProtocol protocol, Socket socket) {
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
        String randomRow = Integer.toString(random.nextInt(3));
        String randomCol = Integer.toString(random.nextInt(3));
        String position = randomRow + "-" + randomCol;
        handledLogMessage("The position chosen was: " + position);
        return position;
    }

    @Override
    protected boolean moveIsValid(String moviment) {
        // Per testejar el servidor titella no vigila on llença, acceptem
        // tots els moviments dins del tauler.
        return true;
    }

    @Override
    protected boolean playAgain() {
        return false;
    }

    @Override
    protected void logMessage(String s) {
        System.out.println(s);
    }
}
