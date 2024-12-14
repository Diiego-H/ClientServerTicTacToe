package p1.server.ai;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import p1.server.GameHandler;
import p1.server.ServerException;
import p1.server.ServerProtocol;
import p1.server.ServerProtocol.ErrorType;
import p1.server.ServerProtocol.MessageType;

public class GameHandlerAI extends GameHandler {

    private Socket socket;
    private ServerProtocol protocol;
    private ServerPartidaAI partida;
    private String player;

    public GameHandlerAI(Socket socket) {
        this.socket = socket;
    }

    @Override
    protected void init() throws IOException {
        this.protocol = createServerProtocol(socket);
        createLogger(String.valueOf(protocol.getSessionID()));
    }

    @Override
    protected MessageType getMessageType() throws ServerException {
        return protocol.getMessageType();
    }

    @Override
    protected void setTimeout(int ms) throws ServerException {
        try {
            socket.setSoTimeout(ms);
        } catch (SocketException e) {
            throw new ServerException("Error setting timeout\n" + e.getMessage());
        }
    }

    @Override
    protected void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            log("Error closing socket\n");
        }
    }

    private void changeState(ServerEstat nextState) {
        estat = nextState;
        errCounter = 0;
    }

    @Override
    protected void checkError(MessageType type) throws ServerException {
        if (type == MessageType.ERROR) {
            String error = protocol.receiveError();
            if (error != null) {
                log("ERROR received: " + error + "\n");
            } else {
                log("SessionID in the received ERROR different from the generated one!\n");
                protocol.sendError(ErrorType.IDSESSIO_INVALID);
                errCounter++;
            }
        } else {
            log("Unexpected opcode received (according to the flow given by the protocol)\n");
            protocol.sendError(ErrorType.COMANDA_INESPERADA);
            errCounter++;

            // Lectura dels camps de trama restants (sense processar)
            protocol.cleanInputData(type);
        }
    }

    @Override
    protected void helloLogic(MessageType type) throws ServerException {
        if (type == MessageType.HELLO) {
            String playerName = protocol.receiveHello();
            if (playerName != null) {
                player = playerName;
                log("HELLO received from " + player + "\n");
                protocol.sendReady();
                log("READY sent\n");
                changeState(ServerEstat.READY);
            } else {
                log("SessionID received from client different from zero!\n");
                protocol.sendError(ErrorType.SESSIO_INCORRECTE);
                errCounter++;
            }
        } else {
            checkError(type);
        }
    }

    @Override
    protected void readyLogic(MessageType type) throws ServerException {
        if (type == MessageType.PLAY) {
            if (protocol.receivePlay()) {
                log("PLAY received\n");
                protocol.sendAdmit(1);
                log("ADMIT sent\n");

                // Creem partida
                partida = new ServerPartidaAI();
                changeState(ServerEstat.PLAY);
                log("TicTacToe match against " + player + " begins!\n\n");
            } else {
                log("SessionID in the received PLAY different from the generated one!\n");
                protocol.sendAdmit(0);
            }
        } else {
            checkError(type);
        }
    }

    private void endGameLogic(String position, boolean playerTurn) throws ServerException {
        int result = partida.getResult();
        protocol.sendResult(playerTurn ? "---" : position, result);
        log("Last position played by " + (playerTurn? player : "ServerAI") + ": " + position + "\n");
        log("Final board:\n" + partida.toString());
        log("Match result: " + result + "\n");
        log("\nWaiting for client for a rematch...\n");

        // Esperem per si el client vol fer una altra partida
        changeState(ServerEstat.READY);
    }

    @Override
    protected void playLogic(MessageType type) throws ServerException {
        if (type == MessageType.ACTION) {
            String position = protocol.receiveAction();
            if (position != null) {
                log("ACTION received. Position: " + position + "\n");
                if (partida.isMove(position)) {
                    if (partida.setMove('X')) {
                        log("Valid move. Current board:\n" + partida.toString() + "\n");
                        if (partida.isFinished()) {
                            endGameLogic(position, true);
                        } else {
                            position = partida.getMove();
                            if (partida.isFinished()) {
                                endGameLogic(position, false);
                            } else {
                                protocol.sendAction(position);
                                log("ACTION sent. Position: " + position + "\n");
                            }
                        }
                    } else {
                        log("Invalid move!\n");
                        protocol.sendError(ErrorType.MOV_INVALID);
                        errCounter++;
                    }
                } else {
                    log("Unknown move!\n");
                    protocol.sendError(ErrorType.MOV_DESCONEGUT);
                    errCounter++;
                }
            } else {
                log("SessionID in the received ACTION different from the generated one!\n");
                protocol.sendError(ErrorType.IDSESSIO_INVALID);
                errCounter++;
            }
        } else {
            checkError(type);
        }
    }

}
