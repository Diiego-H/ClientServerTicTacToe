package p1.server.proxy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

import p1.server.GameHandler;
import p1.server.ServerException;
import p1.server.ServerPartida;
import p1.server.ServerProtocol;
import p1.server.ServerProtocol.ErrorType;
import p1.server.ServerProtocol.MessageType;

/**
 * Classe que gestiona les partides quan el Servidor actua de proxy
 * entre 2 clients. Al no fer multithreading aquesta gestió no es
 * processen les trames d'un jugador fins que no es completi amb èxit
 * el pas del protocol que li toca a l'altre jugador. Podria passar,
 * doncs, que falli la connexió amb un dels dos jugadors i es tanqui
 * el thread que dona servei als clients sense avisar a l'altre. El
 * protocol no detalla què fer en aquesta situació, doncs això és una
 * extensió opcional de la pràctica. Creiem que no serà un problema
 * si podem jugar entre 2 clients (allò demanat explícitament).
 */
public class GameHandlerProxy extends GameHandler {

    // Nombre de clients amb què treballa el servidor
    private static final int N = 2;

    private Socket sockets[];
    private ServerProtocol protocols[];
    private String players[];
    private ServerPartida partida;
    private int player1;
    private int playerHandling;
    private ServerEstat prevEstat;

    public GameHandlerProxy(Socket s0, Socket s1) throws IOException {
        sockets = new Socket[N];
        sockets[0] = s0;
        sockets[1] = s1;
    }

    @Override
    protected void init() throws IOException {
        // Array dels protocols de comunicació
        protocols = new ServerProtocol[N];
        protocols[0] = createServerProtocol(sockets[0]);
        protocols[1] = createServerProtocol(sockets[1]);

        // Array dels noms dels jugadors
        players = new String[2];

        // Decidim quin jugador mourà primer (i es llegiran les seves trames primer)
        chooseOrder();

        prevEstat = ServerEstat.END;

        createLogger(String.valueOf(protocols[0].getSessionID()) + "-" + String.valueOf(protocols[1].getSessionID()));
    }

    // Escollim quin jugador mourà primer aleatòriament
    protected int generateFirstPlayer() {
        return new Random().nextInt(N);
    }

    private void chooseOrder() {
        player1 = generateFirstPlayer();
        playerHandling = player1;
    }

    private boolean isFirstPlayer() {
        return player1 == playerHandling;
    }

    private char getPlayerChar() {
        // El primer jugador en moure a aquesta partida és X)
        return (isFirstPlayer() ? 'X' : 'O');
    }

    private void switchPlayer() {
        playerHandling = 1 - playerHandling;
    }

    // Retornem true si canviem d'estat, false altrament
    private boolean changeState(ServerEstat nextState) {
        boolean b = estat == prevEstat;

        // Només canviem d'estat quan ambdós jugadors completen el pas al protocol
        if (b) {
            estat = nextState;
        } else {
            prevEstat = estat;
        }

        // Processem trames de l'altre jugador i resetegem el comptador d'errors
        switchPlayer();
        errCounter = 0;

        return b;
    }

    @Override
    protected MessageType getMessageType() throws ServerException {
        return protocols[playerHandling].getMessageType();
    }

    @Override
    protected void setTimeout(int ms) throws ServerException {
        try {
            sockets[0].setSoTimeout(ms);
        } catch (SocketException e) {
            throw new ServerException("Error setting timeout for " + players[0] + "\n" + e.getMessage());
        } finally {
            try {
                sockets[1].setSoTimeout(ms);
            } catch (SocketException e) {
                throw new ServerException("Error setting timeout for " + players[1] + "\n" + e.getMessage());
            }
        }
    }

    @Override
    protected void closeConnection() {
        try {
            sockets[0].close();
        } catch (IOException e) {
            log("Error closing socket from player " + players[0] + "\n");
        } finally {
            try {
                sockets[1].close();
            } catch (IOException e) {
                log("Error closing socket from player " + players[1] + "\n");
            }
        }
    }

    @Override
    protected void checkError(MessageType type) throws ServerException {
        if (type == MessageType.ERROR) {
            String error = protocols[playerHandling].receiveError();
            if (error != null) {
                log("ERROR received: " + error + "\n");
            } else {
                log("SessionID in the received ERROR different from the generated one!\n");
                protocols[playerHandling].sendError(ErrorType.IDSESSIO_INVALID);
                errCounter++;
            }
        } else {
            log("Unexpected opcode received (according to the flow given by the protocol)\n");
            protocols[playerHandling].sendError(ErrorType.COMANDA_INESPERADA);
            errCounter++;

            // Lectura dels camps de trama restants (sense processar)
            protocols[playerHandling].cleanInputData(type);
        }
    }

    @Override
    protected void helloLogic(MessageType type) throws ServerException {
        if (type == MessageType.HELLO) {
            String playerName = protocols[playerHandling].receiveHello();
            if (playerName != null) {
                players[playerHandling] = playerName;
                log("HELLO received from " + players[playerHandling] + "\n");
                protocols[playerHandling].sendReady();
                log("READY sent\n");
                changeState(ServerEstat.READY);
            } else {
                log("SessionID received from client different from zero!\n");
                protocols[playerHandling].sendError(ErrorType.SESSIO_INCORRECTE);
                errCounter++;
            }
        } else {
            checkError(type);
        }
    }

    @Override
    protected void readyLogic(MessageType type) throws ServerException {
        if (type == MessageType.PLAY) {
            if (protocols[playerHandling].receivePlay()) {
                log("PLAY received from " + players[playerHandling] + "\n");

                // Determinem si és el 1r o el 2n jugador
                int flag = ((playerHandling == player1) ? 1 : 2);
                protocols[playerHandling].sendAdmit(flag);
                log("ADMIT sent. Flag: " + flag + "\n");

                // Creem partida
                if (changeState(ServerEstat.PLAY)) {
                    partida = new ServerPartida();
                    log(players[playerHandling] + " (X) VS " + players[1-playerHandling] + " (O) TicTacToe match begins!\n\n");
                    log(players[playerHandling].toUpperCase() + " TURN\n");
                }
            } else {
                log("SessionID in the received PLAY different from the generated one!\n");
                protocols[playerHandling].sendAdmit(0);
            }
        } else {
            checkError(type);
        }
    }

    private void endGameLogic(String position) throws ServerException {
        int result = partida.getResult();

        log("\nLast position played by " + players[playerHandling] + ": " + position + "\n");
        log("Final board:\n" + partida.toString());
        log("Match result: " + result + "\n");
        log("\nWaiting for clients for a rematch...\n");

        // Havent acabat la partida només es pot guanyar o empatar
        if (result == 2) {
            /* Empat */

            // A la persona que ha finalitzat el joc li enviem com darrera acció "---"
            protocols[playerHandling].sendResult("---", result);

            // A l'altra la darrera posició col·locada pel contrari
            protocols[1 - playerHandling].sendResult(position, result);
        } else {
            /* Guanya el client que s'està tractant */

            // A la persona que ha finalitzat el joc li enviem com darrera acció "---"
            protocols[playerHandling].sendResult("---", 1);

            // A l'altra la darrera posició col·locada pel contrari
            protocols[1 - playerHandling].sendResult(position, 0);
        }

        // Escollim el nou jugador de la possible propera partida
        chooseOrder();

        // Esperem per si el client vol fer una altra partida
        prevEstat = ServerEstat.END;
        estat = ServerEstat.READY;
        errCounter = 0;
    }

    @Override
    protected void playLogic(MessageType type) throws ServerException {
        if (type == MessageType.ACTION) {
            String position = protocols[playerHandling].receiveAction();
            if (position != null) {
                log("ACTION received. Position: " + position + "\n");
                if (partida.isMove(position)) {
                    if (partida.setMove(getPlayerChar())) {
                        log("Valid move. Current board:\n" + partida.toString());
                        if (partida.isFinished()) {
                            endGameLogic(position);
                        } else {
                            // Reportem el moviment a l'altre client i canviem de torn
                            switchPlayer();
                            protocols[playerHandling].sendAction(position);
                            log("ACTION sent. Position: " + position + "\n\n");
                            log(players[playerHandling].toUpperCase() + " TURN\n");
                        }
                    } else {
                        log("Invalid move!\n");
                        protocols[playerHandling].sendError(ErrorType.MOV_INVALID);
                        errCounter++;
                    }
                } else {
                    log("Unknown move!\n");
                    protocols[playerHandling].sendError(ErrorType.MOV_DESCONEGUT);
                    errCounter++;
                }
            } else {
                log("SessionID in the received ACTION different from the generated one!\n");
                protocols[playerHandling].sendError(ErrorType.IDSESSIO_INVALID);
                errCounter++;
            }
        } else {
            checkError(type);
        }
    }
}
