import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.IllegalBlockingModeException;

import org.junit.Test;

import utils.ComUtils;
import p1.server.Server;
import p1.server.proxy.GameHandlerProxy;

/**
 * Classe per testejar ServerProxy. Fa les mateixes comprovacions que a ServerAITest
 * però intercalant entre els 2 clients de la partida per comprovar que la gestió
 * del canvi de torn realitzada a GameHandlerProxy és correcta.
 */
public class ServerProxyTest extends ServerTest {

    // ServerProxy que accepta una unica parella de clients
    public class VanilaServerProxy extends Server {

        // GameHandlerProxy que gestiona abans el 1r client connectat
        public class NoRandomGameHandler extends GameHandlerProxy {
        
            public NoRandomGameHandler(Socket s0, Socket s1) throws IOException {
                super(s0,s1);
            }

            @Override
            protected int generateFirstPlayer() {
                return 0;
            }
        }

        public VanilaServerProxy(int port) {
            super(port);
        }

        @Override
        public void init() {
            try {
                ss.setSoTimeout(1000);

                // Client 1
                Socket s1 = ss.accept();

                // Client 2 (no perdem la connexió de 1 si falla quelcom)
                Socket s2 = null;
                do {
                    try {
                        s2 = ss.accept();
                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout reached");
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error when accepting 2nd client:\n" + e.getMessage());
                    } catch (SecurityException e) {
                        throw new RuntimeException("Operation not accepted:\n" + e.getMessage());
                    } catch (IllegalBlockingModeException e) {
                        throw new RuntimeException("There is no connection ready to be accepted:\n" + e.getMessage());
                    }
                } while(s2 == null);

                // Executem el mètode run (en aquest Thread)
                (new NoRandomGameHandler(s1,s2)).run();
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout reached");
            } catch (SocketException e) {
                throw new RuntimeException("Unable to set timeout:\n" + e.getMessage());
            } catch (IOException e) {
                throw new RuntimeException("I/O error when accepting 1st client:\n" + e.getMessage());
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
                Server server = new VanilaServerProxy(port);
                server.init();
            }
        }).start();
    }

    private int receive_result(ComUtils comUtils, int sessionID, String lastPos) throws IOException {
        // Recepcio RESULT
        assertEquals(6, comUtils.read_byte());  // RESULT opcode
        assertEquals(sessionID, comUtils.read_int32());  // idSessio generat pel Server
        assertEquals(lastPos, comUtils.read_string(3));  // Moviment guanyador
        return comUtils.read_byte();                     // Resultat
    }

    @Override
    @Test
    public void example_server_test() {
        int port = 8081;
        initServer(port);
        System.out.println();
        Socket connection1 = null;
        Socket connection2 = null;
        try {
            // Test connexio client 1
            setConnection(port);

            // Test connexio client 2
            setConnection(port);

            /* ACTUAL TEST */ 

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection1);
            closeConnection(connection2);
        }
    }

    @Override
    @Test
    public void play_request_test() {
        int port = 8082;
        initServer(port);
        System.out.println();
        Socket connection1 = null;
        Socket connection2 = null;
        try {
            // Test connexio client 1
            connection1 = setConnection(port);
            ComUtils comUtils1 = getComUtils(connection1);

            // Test connexio client 2
            connection2 = setConnection(port);
            ComUtils comUtils2 = getComUtils(connection2);
            
            /* ACTUAL TEST */
            // Enviament HELLO + Recepció READY (client 1)
            int sessionID1 = send_hello_read_ready(comUtils1, "Diego");

            // Enviament HELLO + Recepció READY (client 2)
            int sessionID2 = send_hello_read_ready(comUtils2, "Pol");

            // Enviament PLAY + Recepció ADMIT (client 1 amb flag 1)
            send_play_read_admit(comUtils1, sessionID1, 1);

            // Enviament PLAY + Recepció ADMIT (client 2 amb flag 2)
            send_play_read_admit(comUtils2, sessionID2, 2);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection1);
            closeConnection(connection2);
        }
    }

    @Override
    @Test
    public void error_hello_test() {
        int port = 8083;
        initServer(port);
        System.out.println();
        Socket connection1 = null;
        Socket connection2 = null;
        try {
            // Test connexio client 1
            connection1 = setConnection(port);
            ComUtils comUtils1 = getComUtils(connection1);

            // Test connexio client 2
            connection2 = setConnection(port);
            ComUtils comUtils2 = getComUtils(connection2);
            
            /* ACTUAL TEST */ 
            // Enviament HELLO (de client 1, incorrecte)
            send_hello_incorrect(comUtils1, "Diego");

            // Recepcio ERROR (a client 1. Codi d'error 9 i missatge "Sessio Incorrecte")
            int sessionID1 = receive_error(comUtils1, 9, "Sessio Incorrecte");

            // Enviament HELLO + Recepció READY (client 1)
            assertEquals(sessionID1, send_hello_read_ready(comUtils1, "Diego"));

            // Enviament PLAY (client 2 SENSE HELLO!)
            send_play(comUtils2, sessionID1);

            // Recepcio ERROR (Codi d'error 2 i missatge "Comanda Inesperada")
            receive_error(comUtils2, 2, "Comanda Inesperada");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection1);
            closeConnection(connection2);
        }
    }

    @Override
    @Test
    public void error_play_test() {
        int port = 8084;
        initServer(port);
        System.out.println();
        Socket connection1 = null;
        Socket connection2 = null;
        try {
            // Test connexio client 1
            connection1 = setConnection(port);
            ComUtils comUtils1 = getComUtils(connection1);

            // Test connexio client 2
            connection2 = setConnection(port);
            ComUtils comUtils2 = getComUtils(connection2);
            
            /* ACTUAL TEST */ 
            int sessionID1 = send_hello_read_ready(comUtils1, "Diego");
            int sessionID2 = send_hello_read_ready(comUtils2, "Pol");

            // Enviament PLAY + Recepció ADMIT (client 1, sessionID modificat, flag 0)
            send_play_read_admit(comUtils1, sessionID1-1, 0);

            // Enviament PLAY i recepció ADMIT (client 1, flag 1)
            send_play_read_admit(comUtils1, sessionID1, 1);

            // Enviament HELLO (client 2, incorrecte doncs el servidor espera PLAY)
            send_hello_correct(comUtils2, "Pol");

            // Recepcio ERROR (Codi d'error 2 i missatge "Comanda Inesperada")
            receive_error(comUtils2, 2, "Comanda Inesperada");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection1);
            closeConnection(connection2);
        }
    }

    @Override
    @Test
    public void action_result_test() {
        int port = 8085;
        initServer(port);
        System.out.println();
        Socket connection1 = null;
        Socket connection2 = null;
        try {
            // Test connexio client 1
            connection1 = setConnection(port);
            ComUtils comUtils1 = getComUtils(connection1);

            // Test connexio client 2
            connection2 = setConnection(port);
            ComUtils comUtils2 = getComUtils(connection2);

            /* ACTUAL TEST */ 
            // Inici de partida
            int sessionID1 = send_hello_read_ready(comUtils1, "Diego");
            int sessionID2 = send_hello_read_ready(comUtils2, "Pol");
            send_play_read_admit(comUtils1, sessionID1, 1);
            send_play_read_admit(comUtils2, sessionID2, 2);

            // Enviament ACTION (client 1)
            send_action(comUtils1, sessionID1, "1-0");

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                | X |   |   |
                |-----------|
                |   |   |   |
                 --- --- ---   */

            // Recepcio ACTION (client 2)
            String mov = receive_action(comUtils2, sessionID2);

            // Comprovacio moviment
            assertEquals('-', mov.charAt(1));
            int i = Integer.parseInt(mov.substring(0, 1));
            assertTrue(i >= 0 && i <= 2);
            int j = Integer.parseInt(mov.substring(2, 3));
            assertTrue(j >= 0 && j <= 2);

            // Enviament ACTION (client 2)
            send_action(comUtils2, sessionID2, "2-0");

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                | X |   |   |
                |-----------|
                | O |   |   |
                 --- --- ---   */

            // Recepcio ACTION (client 1)
            receive_action(comUtils1, sessionID1);

            // Enviament ACTION (client 1)
            send_action(comUtils1, sessionID1, "1-1");

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                | X | X |   |
                |-----------|
                | O |   |   |
                 --- --- ---   */

            // Recepcio ACTION (client 2)
            receive_action(comUtils2, sessionID2);

            // Enviament ACTION (client 2)
            send_action(comUtils2, sessionID2, "2-1");

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                | X | X |   |
                |-----------|
                | O | O |   |
                 --- --- ---   */

            // Recepcio ACTION (client 1)
            receive_action(comUtils1, sessionID1);

            // Enviament ACTION (client 1)
            send_action(comUtils1, sessionID1, "1-2");    // Moviment guanyador

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                | X | X | X |
                |-----------|
                | O | O |   |
                 --- --- ---   */

            // Recepcio RESULT (client 1, "---" perquè ha guanyat la partida)
            receive_result(comUtils1, sessionID1, "---");

            // Recepcio RESULT (client 2, "1-2" perquè rep el moviment guanyador de client 1)
            receive_result(comUtils2, sessionID2, "1-2");

            // Comprovem que podem tornar a jugar
            // Enviament PLAY + Recepció ADMIT (client 1)
            send_play_read_admit(comUtils1, sessionID1, 1);

            // Enviament PLAY + Recepció ADMIT (client 2)
            send_play_read_admit(comUtils2, sessionID2, 2);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection1);
            closeConnection(connection2);
        }
    }

    @Override
    @Test
    public void error_action_test() {
        int port = 8086;
        initServer(port);
        System.out.println();
        Socket connection1 = null;
        Socket connection2 = null;
        try {
            // Test connexio client 1
            connection1 = setConnection(port);
            ComUtils comUtils1 = getComUtils(connection1);

            // Test connexio client 2
            connection2 = setConnection(port);
            ComUtils comUtils2 = getComUtils(connection2);

            /* ACTUAL TEST */ 
            // Inici de partida
            int sessionID1 = send_hello_read_ready(comUtils1, "Diego");
            int sessionID2 = send_hello_read_ready(comUtils2, "Pol");
            send_play_read_admit(comUtils1, sessionID1, 1);
            send_play_read_admit(comUtils2, sessionID2, 2);
            
            // Enviament PLAY (client 1: incorrecte doncs el servidor espera ACTION)
            send_play(comUtils1, sessionID1);

            // Recepcio ERROR (client 1: Codi d'error 2 i missatge "Comanda Inesperada")
            receive_error(comUtils1, 2, "Comanda Inesperada");

            // Enviament ACTION (client 1)
            send_action(comUtils1, sessionID1, "1-1");

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                |   | X |   |
                |-----------|
                |   |   |   |
                 --- --- ---   */

            // Recepcio ACTION (client 2)
            receive_action(comUtils2, sessionID2);

            // Enviament ACTION (client 2: idSessio incorrecte)
            send_action(comUtils2, sessionID2-1, "0-0");

            // Recepcio ERROR (client 2: Codi d'error 3 i missatage "ID Sessio incorrecte")
            receive_error(comUtils2, 3, "ID Sessio Incorrecte");

            // Enviament ACTION (client 2: moviment desconegut)
            send_action(comUtils2, sessionID2, "a-0");

            // Recepcio ERROR (client 2: Codi d'error 0 i missatge "Moviment Desconegut")
            receive_error(comUtils2, 0, "Moviment Desconegut");

            // Enviament ACTION (client 2)
            send_action(comUtils2, sessionID2, "2-1");

            /**  --- --- --- 
                |   |   |   |
                |-----------|
                |   | X |   |
                |-----------|
                |   | O |   |
                 --- --- ---   */

            // Recepcio ACTION (client 1)
            receive_action(comUtils1, sessionID1);
            
            // Enviament ACTION (client 1: moviment desconegut)
            send_action(comUtils1, sessionID1, "1-7");

            // Recepcio ERROR (client 1: Codi d'error 0 i missatge "Moviment Desconegut")
            receive_error(comUtils1, 0, "Moviment Desconegut");

            // Enviament ACTION (client 1: moviment invalid)
            send_action(comUtils1, sessionID1, "1-1");

            // Recepcio ERROR (client 1: Codi d'error 1 i missatge "Moviment Invalid")
            receive_error(comUtils1, 1, "Moviment Invalid");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection(connection1);
            closeConnection(connection2);
        }
    }
}
