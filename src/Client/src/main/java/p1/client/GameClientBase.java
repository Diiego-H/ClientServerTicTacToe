package p1.client;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import p1.client.ClientProtocol.ErrorType;
import p1.client.Trames.ErrorTrama;
import p1.client.Trames.ResultTrama;

abstract class GameClientBase {

    protected ClientProtocol protocol;
    protected Socket socket;
    protected ClientStateMachine stateMachine;
    protected int sessionId;
    protected TicTacToeBoard board;
    private int opcode = 0;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public GameClientBase(ClientProtocol protocol, Socket socket) {
        this.protocol = protocol;
        this.socket = socket;
        this.stateMachine = new ClientStateMachine();
        this.board = new TicTacToeBoard();
    }

    protected abstract String obtainPlayerName();

    protected abstract String obtainPosition();

    protected abstract boolean moveIsValid(String moviment);

    protected abstract boolean playAgain();

    protected void initLogger(String playerName) throws IOException {
    }

    protected void closeLogger() throws IOException {
    }

    protected abstract void logMessage(String s) throws IOException;

    protected void handledLogMessage(String s) {
        try {
            logMessage(s);
        } catch (IOException IOException) {
            System.out.println(IOException.getMessage());
        }
    }

    private void writeIntro() {
        handledLogMessage("                       TIC-TAC-TOE");
        handledLogMessage("The game board consists of a grid of 3x3 squares,");
        handledLogMessage("which are indexed by row and column with numbers from 0 to 2.");
        handledLogMessage("The objective of the game is to place three symbols in");
        handledLogMessage("any horizontal, vertical, or diagonal line before");
        handledLogMessage("the other player does.");
        handledLogMessage("If all squares are filled and no player has managed");
        handledLogMessage("to form a line of three symbols, the game ends in a draw.");
        handledLogMessage("You will play with the X symbol, and your enemy will");
        handledLogMessage("play with the O symbol.");
    }

    private boolean getOpcode() throws IOException {
        // Llegim el opcode amb un timeout, que si expira causara una desconnexio.
        try {
            socket.setSoTimeout(10000);
            opcode = protocol.readOpCode();
            return false;
        } catch (SocketTimeoutException timeoutException) {
            handledLogMessage("There has been a timeout");
            return true;
        } catch (SocketException socketException) {
            handledLogMessage("Error setting the timeout");
            return true;
        }
    }

    public void startGame() {
        boolean disconnect = false;
        while (!disconnect) {
            switch (stateMachine.getCurrentState()) {
                case HELLO_READY:
                    try {
                        // Comencem enviant un Hello.
                        String playerName = obtainPlayerName();

                        try {
                            initLogger(playerName);
                        } catch (IOException IOException) {
                            // Si hi ha hagut un error creant el logger ens desconnectem.
                            System.out.println("Error creant el logger: " + IOException.getMessage());
                            disconnect = true;
                            break;
                        }

                        handledLogMessage("SESSION STARTED (" + LocalDateTime.now().format(formatter) + ")\n");
                        writeIntro();

                        protocol.sendHello(0, playerName);

                        // Llegim l'opcode de la seguent trama que s'envii.
                        disconnect = getOpcode();
                        if (disconnect)
                            break;

                        if (opcode == ClientProtocol.MessageType.READY.getValue()) {
                            // Llegim la resta del missatge Ready i actualitzem el sessionId.
                            this.sessionId = protocol.readReady();

                            // Informem a la maquina d'estats que s'ha rebut el ready.
                            stateMachine.performAction(ClientStateMachine.Action.READY_RECEIVED);
                        }

                        else if (opcode == ClientProtocol.MessageType.ERROR.getValue()) {
                            // Informem a la maquina d'estats que s'ha rebut un error.
                            stateMachine.performAction(ClientStateMachine.Action.HELLO_READY_ERROR_RECEIVED);
                        }

                        else {
                            // Si l'opcode no té sentit en aquest estat, acabem de llegir el missatge.
                            protocol.readAny(opcode);
                        }
                    }

                    catch (Exception IOException) {
                        handledLogMessage(IOException.getMessage());
                        disconnect = true;
                    }
                    break;

                // Els tres estats d'error es tracten de la mateixa manera.
                case HANDLE_HELLO_READY_ERROR:
                case HANDLE_PLAYING_ERROR:
                case HANDLE_PLAY_ADMIT_ERROR:
                    try {
                        // Llegim la resta de la trama de Error i imprimim el missatge d'error.
                        ErrorTrama errorTrama = protocol.readError();
                        handledLogMessage(errorTrama.getErrorMessage());
                        stateMachine.performAction(ClientStateMachine.Action.ERROR_MESSAGE_PRINTED);
                    }

                    catch (Exception IOException) {
                        handledLogMessage(IOException.getMessage());
                        disconnect = true;
                    }
                    break;

                case PLAY_ADMIT:
                    try {
                        protocol.sendPlay(sessionId);
                        disconnect = getOpcode();
                        if (disconnect)
                            break;

                        if (opcode == ClientProtocol.MessageType.ADMIT.getValue()) {
                            // Llegim la resta de la trama Admit
                            int flag = protocol.readAdmit();

                            if (flag == 0) {
                                // El sessionId enviat és incorrecte: ens desconnectem.
                                disconnect = true;
                            }

                            else if (flag == 1) {
                                // La partida ha sigut admesa i juguem al primer torn
                                stateMachine.performAction(ClientStateMachine.Action.ADMIT_RECEIVED);
                                handledLogMessage("\nGAME STARTED (" + LocalDateTime.now().format(formatter) + ")\n");
                                handledLogMessage("Starting board:");
                            }

                            else if (flag == 2) {
                                // La partida ha sigut admesa i juguem al segon torn, processem el primer
                                disconnect = getOpcode();
                                if (disconnect)
                                    break;

                                handledLogMessage("\nGAME STARTED (" + LocalDateTime.now().format(formatter) + ")\n");
                                handledLogMessage("\nStarting board:");
                                if (opcode == ClientProtocol.MessageType.ACTION.getValue()) {
                                    String moviment = protocol.readAction();

                                    if (board.moveIsKnown(moviment)) {
                                        if (moveIsValid(moviment)) {
                                            // Li passem el moviment al tauler perque l'actualitzi
                                            board.makeMove(moviment, 'O');
                                        } else {
                                            // Enviem una trama d'error: Moviment Invalid.
                                            protocol.sendError(sessionId, ErrorType.MOV_INVALID);
                                        }
                                    } else {
                                        // Enviem una trama d'error: Moviment Desconegut.
                                        protocol.sendError(sessionId, ErrorType.MOV_DESCONEGUT);
                                    }
                                }

                                stateMachine.performAction(ClientStateMachine.Action.ADMIT_RECEIVED);
                            }
                        }

                        else if (opcode == ClientProtocol.MessageType.ERROR.getValue()) {
                            // Informem a la maquina d'estats que s'ha rebut un error.
                            stateMachine.performAction(ClientStateMachine.Action.PLAY_ADMIT_ERROR_RECEIVED);
                        }

                        else {
                            // Si l'opcode no té sentit en aquest estat, acabem de llegir el missatge.
                            protocol.readAny(opcode);
                        }

                    } catch (Exception IOException) {
                        handledLogMessage(IOException.getMessage());
                        disconnect = true;
                    }
                    break;

                case PLAYING:
                    try {
                        // Imprimim el tauler
                        handledLogMessage(board.boardToString());

                        String position = obtainPosition();

                        protocol.sendAction(sessionId, position);

                        boolean tramaValidaRebuda = false;

                        while (!tramaValidaRebuda) {
                            // Llegim el opcode de la trama seguent
                            disconnect = getOpcode();
                            if (disconnect)
                                break;

                            if (opcode == ClientProtocol.MessageType.ERROR.getValue()) {
                                tramaValidaRebuda = true;
                                stateMachine.performAction(ClientStateMachine.Action.PLAYING_ERROR_RECEIVED);
                            }

                            else {
                                // Si el missatge rebut no és d'error, actualitzem el tauler
                                board.makeMove(position, 'X');
                                if (opcode == ClientProtocol.MessageType.ACTION.getValue()) {
                                    String moviment = protocol.readAction();

                                    if (board.moveIsKnown(moviment)) {

                                        if (moveIsValid(moviment)) {
                                            tramaValidaRebuda = true;
                                            // Li passem el moviment al tauler perque l'actualitzi
                                            board.makeMove(moviment, 'O');
                                        } else {
                                            // Enviem una trama d'error: Moviment Invalid.
                                            protocol.sendError(sessionId, ErrorType.MOV_INVALID);
                                        }
                                    } else {
                                        // Enviem una trama d'error: Moviment Desconegut.
                                        protocol.sendError(sessionId, ErrorType.MOV_DESCONEGUT);
                                    }
                                }

                                else if (opcode == ClientProtocol.MessageType.RESULT.getValue()) {
                                    tramaValidaRebuda = true;
                                    stateMachine.performAction(ClientStateMachine.Action.RESULT_RECEIVED);
                                }

                                else {
                                    // Si l'opcode no té sentit en aquest estat, acabem de llegir el missatge.
                                    protocol.readAny(opcode);
                                    tramaValidaRebuda = true;
                                }
                            }
                        }
                    } catch (Exception IOException) {
                        handledLogMessage(IOException.getMessage());
                        disconnect = true;
                    }
                    break;

                case END:
                    try {
                        // Llegim la resta de la trama de Result
                        ResultTrama result = protocol.readResult();

                        // Si el servidor ha mogut i el moviment és valid, actualitzem el tauler
                        String moviment = result.getMoviment();
                        if (board.moveIsKnown(moviment) && moveIsValid(moviment)) {
                            board.makeMove(moviment, 'O');
                        }
                        handledLogMessage("Final board:");
                        handledLogMessage(board.boardToString());

                        switch (result.getFlag()) {
                            case 0:
                                handledLogMessage("You lost :(");
                                break;
                            case 1:
                                handledLogMessage("You won!!!");
                                break;
                            case 2:
                                handledLogMessage("You have tied.");
                                break;
                            default:
                                break;
                        }

                        handledLogMessage("\nGAME FINISHED (" + LocalDateTime.now().format(formatter) + ")\n");

                        // Decidim si volem jugar una altra partida
                        if (playAgain()) {
                            handledLogMessage("You have decided to play again.");
                            // Reiniciem el tauler i anem a l'estat PLAY_ADMIT
                            board = new TicTacToeBoard();
                            stateMachine.performAction(ClientStateMachine.Action.PLAY_AGAIN);
                        } else {
                            handledLogMessage("You have decided to stop playing.");
                            // Ens desconnectem
                            disconnect = true;
                        }
                    }

                    catch (Exception IOException) {
                        handledLogMessage(IOException.getMessage());
                        disconnect = true;
                    }
                    break;
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            handledLogMessage(e.getMessage());
            handledLogMessage("Error closing socket");
        } finally {
            handledLogMessage("\nSESSION FINISHED (" + LocalDateTime.now().format(formatter) + ")\n");
        }

        try {
            closeLogger();
        } catch (IOException IOException) {
            System.out.println("Error closing the logger: " + IOException.getMessage());
        }
    }
}
