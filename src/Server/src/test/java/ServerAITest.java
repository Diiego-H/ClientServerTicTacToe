import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;

import org.junit.Test;

import utils.ComUtils;
import p1.server.Server;
import p1.server.ai.GameHandlerAI;

public class ServerAITest extends ServerTest {

    private static final String PLAYER = "Diego";

    // ServerAI que accepta un unic client
    public class VanilaServerAI extends Server {

        public VanilaServerAI(int port) {
            super(port);
        }

        @Override
        public void init() {
            try {
                ss.setSoTimeout(1000);
                Socket socket = ss.accept();
                (new GameHandlerAI(socket)).run();
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout reached");
            } catch (SocketException e) {
                throw new RuntimeException("Unable to set timeout:\n" + e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException("I/O error when accepting a client:\n" + e.getMessage());
            } catch (SecurityException e) {
                throw new RuntimeException("Operation not accepted:\n" + e.getMessage());
            } catch (IllegalBlockingModeException e) {
                throw new RuntimeException("There is no connection ready to be accepted:\n" + e.getMessage());
            } finally {
                try {
                    ss.close();
                } catch (IOException e) {
                    System.out.println("Problem closing ServerSocket:\n" + e.getMessage());
                } finally {
                    System.out.println("ServerSocket closed");
                }
            }
        }
    }

    @Override
    protected void initServer(int port) {
        (new Thread() {
            public void run() {
                new VanilaServerAI(port).init();
            }
        }).start();
    }

    private void send_hello_incorrect(ComUtils comUtils) throws IOException {
        super.send_hello_incorrect(comUtils, PLAYER);
    }

    private int send_hello_read_ready(ComUtils comUtils) throws IOException {
        return super.send_hello_read_ready(comUtils, PLAYER);
    }

    private String send_receive_action(ComUtils comUtils, int sessionID, String pos) throws IOException {
        send_action(comUtils, sessionID, pos);
        return receive_action(comUtils, sessionID);
    }

    private int receive_result(ComUtils comUtils, int sessionID) throws IOException {
        // Recepcio RESULT
        assertEquals(6, comUtils.read_byte());  // RESULT opcode
        assertEquals(sessionID, comUtils.read_int32());  // idSessio generat pel Server
        comUtils.read_string(3);                         // Moviment guanyador
        return comUtils.read_byte();                     // Resultat
    }

    private int send_action_receive_result(ComUtils comUtils, int sessionID, String pos) throws IOException {
        send_action(comUtils, sessionID, pos);
        return receive_result(comUtils, sessionID);
    }

    @Override
    @Test
    public void example_server_test() {
        int port = 8081;
        initServer(port);
        System.out.println();
        Socket connection = null;
        try {
            connection = setConnection(port);
            ComUtils comUtils = getComUtils(connection);

            /* ACTUAL TEST */ 

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    @Test
    public void play_request_test() {
        int port = 8082;
        initServer(port);
        System.out.println();
        Socket connection = null;
        try {
            connection = setConnection(port);
            ComUtils comUtils = getComUtils(connection);

            /* ACTUAL TEST */ 
            // Enviament HELLO + Recepció READY
            int sessionID = send_hello_read_ready(comUtils);

            // Enviament PLAY + Recepció ADMIT (flag 1)
            send_play_read_admit(comUtils, sessionID, 1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    @Test
    public void error_hello_test() {
        int port = 8083;
        initServer(port);
        System.out.println();
        Socket connection = null;
        try {
            connection = setConnection(port);
            ComUtils comUtils = getComUtils(connection);

            /* ACTUAL TEST */ 
            // Enviament HELLO (incorrecte)
            send_hello_incorrect(comUtils);

            // Recepcio ERROR (Codi d'error 9 i missatge "Sessio Incorrecte")
            int sessionID = receive_error(comUtils, 9, "Sessio Incorrecte");

            // Enviament PLAY (incorrecte doncs no hem rebut READY)
            send_play(comUtils, sessionID);

            // Recepcio ERROR (Codi d'error 2 i missatge "Comanda Inesperada")
            assertEquals(sessionID, receive_error(comUtils, 2, "Comanda Inesperada"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    @Test
    public void error_play_test() {
        int port = 8084;
        initServer(port);
        System.out.println();
        Socket connection = null;
        try {
            connection = setConnection(port);
            ComUtils comUtils = getComUtils(connection);

            /* ACTUAL TEST */ 
            int sessionID = send_hello_read_ready(comUtils);

            // Enviament PLAY + Recepció ADMIT (flag 0 al modificar el sessionID)
            send_play_read_admit(comUtils, sessionID-1, 0);

            // Enviament HELLO (incorrecte doncs el servidor espera PLAY)
            send_hello_incorrect(comUtils);
            
            // Recepcio ERROR (Codi d'error 2 i missatge "Comanda Inesperada")
            assertEquals(sessionID, receive_error(comUtils, 2, "Comanda Inesperada"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    @Test
    public void action_result_test() {
        int port = 8085;
        initServer(port);
        System.out.println();
        Socket connection = null;
        try {
            connection = setConnection(port);
            ComUtils comUtils = getComUtils(connection);

            /* ACTUAL TEST */ 
            // Inici de partida
            int sessionID = send_hello_read_ready(comUtils);
            send_play_read_admit(comUtils, sessionID, 1);

            // Enviament ACTION (client) + Recepció ACTION (servidor)
            String mov = send_receive_action(comUtils, sessionID, "1-0");

            // Comprovacio moviment
            assertEquals('-', mov.charAt(1));
            int i = Integer.parseInt(mov.substring(0, 1));
            assertTrue(i >= 0 && i <= 2);
            int j = Integer.parseInt(mov.substring(2, 3));
            assertTrue(j >= 0 && j <= 2);
            assertFalse(i == 1 && j == 0);

            // Enviament ACTION (client) + Recepció ACTION (servidor)
            send_receive_action(comUtils, sessionID, "2-0");

            // Enviament ACTION (client) + Recepció RESULT (servidor)
            int result = send_action_receive_result(comUtils, sessionID, "1-1");
            assertEquals(0, result);    // Server guanyador

            // Comprovem que podem tornar a jugar
            send_play_read_admit(comUtils, sessionID, 1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }

    @Override
    @Test
    public void error_action_test() {
        int port = 8086;
        initServer(port);
        System.out.println();
        Socket connection = null;
        try {
            connection = setConnection(port);
            ComUtils comUtils = getComUtils(connection);

            /* ACTUAL TEST */ 
            // Inici de partida
            int sessionID = send_hello_read_ready(comUtils);
            send_play_read_admit(comUtils, sessionID, 1);

            // Enviament PLAY (incorrecte doncs no hem rebut ACTION)
            send_play(comUtils, sessionID);

            // Recepcio ERROR (Codi d'error 2 i missatge "Comanda Inesperada")
            assertEquals(sessionID, receive_error(comUtils, 2, "Comanda Inesperada"));

            // Enviament ACTION (idSessio incorrecte)
            send_action(comUtils, sessionID-1, "0-0");

            // Recepcio ERROR (Codi d'error 3 i missatge "ID Sessio Incorrecte")
            receive_error(comUtils, 3, "ID Sessio Incorrecte");

            // Enviament ACTION (moviment desconegut)
            send_action(comUtils, sessionID, "a-0");

            // Recepcio ERROR (Codi d'error 0 i missatge "Moviment Desconegut")
            receive_error(comUtils, 0, "Moviment Desconegut");
            
            // Enviament ACTION (moviment desconegut)
            send_action(comUtils, sessionID, "1-7");

            // Recepcio ERROR (Codi d'error 0 i missatge "Moviment Desconegut")
            receive_error(comUtils, 0, "Moviment Desconegut");

            // Fem un moviment i al següent torn el repetim (moviment invalid)
            send_receive_action(comUtils, sessionID, "0-0");
            send_action(comUtils, sessionID, "0-0");

            // Recepcio ERROR (Codi d'error 1 i missatge "Moviment Invalid")
            receive_error(comUtils, 1, "Moviment Invalid");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection);
        }
    }
}
