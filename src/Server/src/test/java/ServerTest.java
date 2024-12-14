import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.Socket;

import utils.ComUtils;

// Especificació de classe de test del servidor (pels modes AI i Proxy)
public abstract class ServerTest {
    
    /* Funcions de connexió */
    protected abstract void initServer(int port);

    protected void closeConnection(Socket s) {
        try {
            s.close();
        } catch (Exception e) {
            System.out.println("Unable to close socket");
        } finally {
            System.out.println("Connection closed");
        }
    }

    protected Socket setConnection(int port) {
        Socket connection = null;
        do {
            try {
                connection = new Socket("localhost", port);
            } catch (Exception e) {}
        } while(connection == null);
        assertNotNull(connection);
        return connection;
    }

    protected ComUtils getComUtils(Socket connection) throws IOException {
        return new ComUtils(connection.getInputStream(), connection.getOutputStream());
    }

    /* Enviament-Comprovació trames */
    private void send_hello(ComUtils comUtils, int sessionID, String player) throws IOException {
        // Enviament HELLO
        comUtils.write_byte(1);                    // HELLO opcode
        comUtils.write_int32(sessionID);           // idSessio
        comUtils.write_variable_string(player);    // nomJugador
    }

    protected void send_hello_correct(ComUtils comUtils, String player) throws IOException {
        send_hello(comUtils, 0, player);     // idSessio 0
    }

    protected void send_hello_incorrect(ComUtils comUtils, String player) throws IOException {
        send_hello(comUtils, 12345, player); // idSessio NO 0
    }

    protected int send_hello_read_ready(ComUtils comUtils, String player) throws IOException {
        send_hello_correct(comUtils, player);

        assertEquals(2, comUtils.read_byte());  // READY opcode
        int sessionID = comUtils.read_int32();           // idSessio generat pel Server
        assert(((int)(Math.log10(sessionID) + 1)) == 5); // Check idSessio
        return sessionID;
    }

    protected void send_play(ComUtils comUtils, int sessionID) throws IOException {
        // Enviament PLAY
        comUtils.write_byte(3);                         // PLAY opcode
        comUtils.write_int32(sessionID);                // idSessio generat pel Server
    }

    protected void send_play_read_admit(ComUtils comUtils, int sessionID, int flag) throws IOException {
        send_play(comUtils, sessionID);

        // Recepcio ADMIT
        assertEquals(4, comUtils.read_byte());     // ADMIT opcode
        int receivedID = comUtils.read_int32();             // idSessio generat pel Server
        if (flag == 1) {
            assertEquals(sessionID, receivedID); 
        }
        assertEquals(flag, comUtils.read_byte());           // idSessio correcte al PLAY
    }

    protected int receive_error(ComUtils comUtils, int errCode, String errMsg) throws IOException {
        assertEquals(8, comUtils.read_byte());        // ERROR opcode
        int sessionID = comUtils.read_int32();                 // idSessio generat pel Server
        assertEquals(errCode, comUtils.read_byte());           // Codi d'error enviat
        assertEquals(errMsg, comUtils.read_variable_string()); // Missatge d'error
        return sessionID;
    }

    protected void send_action(ComUtils comUtils, int sessionID, String pos) throws IOException {
        // Enviament ACTION
        comUtils.write_byte(5);             // ACTION opcode
        comUtils.write_int32(sessionID);    // idSessio generat pel Server
        comUtils.write_string(pos);         // Moviment
    }

    protected String receive_action(ComUtils comUtils, int sessionID) throws IOException {
        // Recepcio ACTION
        assertEquals(5, comUtils.read_byte());  // ACTION opcode  
        comUtils.read_int32();                           // idSessio generat pel Server
        return comUtils.read_string(3);                  // Moviment
    }

    /* Testos */
    public abstract void example_server_test();
    public abstract void play_request_test();
    public abstract void error_hello_test();
    public abstract void error_play_test();
    public abstract void action_result_test();
    public abstract void error_action_test();
}
