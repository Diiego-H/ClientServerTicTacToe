import org.junit.Test;
import static org.junit.Assert.*;
import utils.ComUtils;

import p1.client.Client;
import p1.client.ClientProtocol;
import p1.client.GameClientTest;
import p1.client.ClientProtocol.ErrorType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;

public class ClientTest {

    abstract class TestingServer extends Thread {

        protected int port;
        protected ServerSocket ss;
        protected Socket socket;
        protected ComUtils comutils;

        public TestingServer(int port) {
            this.port = port;
            setConnection();
        }

        public void run() {
            this.init();
        }

        public ComUtils getComutils(Socket socket) {
            if (comutils == null) {
                try {
                    comutils = new ComUtils(socket.getInputStream(), socket.getOutputStream());
                } catch (IOException e) {
                    throw new RuntimeException("I/O Error when creating the ComUtils:\n" + e.getMessage());
                }
            }
            return comutils;
        }

        public void setConnection() {
            if (this.ss == null) {
                try {
                    ss = new ServerSocket(port);
                    System.out.println("Server up & listening on port " + port + "...\nPress Cntrl + C to stop.");
                } catch (IOException e) {
                    throw new RuntimeException("I/O error when opening the Server Socket:\n" + e.getMessage());
                }
            }
        }

        public Socket getSocket() {
            return this.socket;
        }

        private void init() {
            try {
                socket = ss.accept();
                comutils = getComutils(socket);
                System.out.println("Client accepted");

                executeTest(comutils);

                System.out.println("Closing server...");
                ss.close();

            } catch (IOException e) {
                throw new RuntimeException("I/O error when accepting a client:\n" + e.getMessage());
            } catch (SecurityException e) {
                throw new RuntimeException("Operation not accepted:\n" + e.getMessage());
            } catch (IllegalBlockingModeException e) {
                throw new RuntimeException("There is no connection ready to be accepted:\n" + e.getMessage());
            }
        }

        protected abstract void executeTest(ComUtils comutils) throws IOException;
    }

    class ServerHello extends TestingServer {

        public ServerHello(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException {
            // Manage different tests
            int opcode = comutils.read_int32();
            comutils.write_int32(opcode);
            comutils.write_int32(comutils.read_int32());
            String s1 = comutils.read_variable_string();
            comutils.write_variable_string(s1);

            System.out.println("Closing server...");
            socket.close();
        }
    }

    class ServerAction extends TestingServer {

        public ServerAction(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException {
            // Manage different tests
            comutils.write_byte(comutils.read_byte());
            comutils.write_int32(comutils.read_int32());
            comutils.write_string(comutils.read_string(3));

            System.out.println("Closing server...");
            socket.close();
        }
    }

    class ServerError extends TestingServer {

        public ServerError(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException {
            comutils.write_byte(comutils.read_byte());
            comutils.write_int32(comutils.read_int32());
            comutils.write_byte(comutils.read_byte());
            comutils.write_variable_string(comutils.read_variable_string());

            System.out.println("Closing server...");
            socket.close();
        }
    }

    class ServerPlay extends TestingServer {

        public ServerPlay(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException {
            // Manage different tests
            comutils.write_byte(comutils.read_byte());
            comutils.write_int32(comutils.read_int32());

            System.out.println("Closing server...");
            socket.close();
        }
    }

    class ServerDeniedAdmit extends TestingServer {

        public ServerDeniedAdmit(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException {
            // Rebem el hello
            assertEquals(1, comutils.read_byte());
            assertEquals(0, comutils.read_int32());
            assertEquals("JohannesTheGreat444", comutils.read_variable_string());

            // Enviem un ready
            comutils.write_byte(2);
            comutils.write_int32(11111);

            System.out.println("Ready Sent");

            // Rebem un play
            assertEquals(3, comutils.read_byte());
            assertEquals(11111, comutils.read_int32());

            System.out.println("Play Received");

            // Enviem un admit
            comutils.write_byte(4);
            comutils.write_int32(11111);
            comutils.write_byte(0);

            System.out.println("Denied Admit Sent");

            // L'admit enviat té flag 0, el client s'ha de desconnectar sol.

            System.out.println("Closing server...");
            socket.close();
            System.out.println("Socket has been closed");
        }
    }

    class ServerFullGame extends TestingServer {

        public ServerFullGame(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException {
            // Rebem el hello
            assertEquals(1, comutils.read_byte());
            assertEquals(0, comutils.read_int32());
            assertEquals("JohannesTheGreat444", comutils.read_variable_string());

            // Enviem un ready
            comutils.write_byte(2);
            comutils.write_int32(11111);

            System.out.println("Ready Sent");

            // Rebem un play
            assertEquals(3, comutils.read_byte());
            assertEquals(11111, comutils.read_int32());

            System.out.println("Play Received");

            // Enviem un admit
            comutils.write_byte(4);
            comutils.write_int32(11111);
            comutils.write_byte(1);

            System.out.println("Approved Admit Sent");

            for (int i = 0; i < 10; i++) {
                // Rebem un action
                assertEquals(5, comutils.read_byte());
                assertEquals(11111, comutils.read_int32());
                // No puc fer un assert aquí ja que el client mou aleatòriament
                comutils.read_string(3);

                // Enviem un action
                comutils.write_byte(5);
                comutils.write_int32(11111);
                String row = Integer.toString(i % 3);
                String col = Integer.toString((i / 3) % 3);
                String position = row + "-" + col;
                comutils.write_string(position);
            }

            // Rebem un action
            assertEquals(5, comutils.read_byte());
            assertEquals(11111, comutils.read_int32());
            // No puc fer un assert aquí ja que el client mou aleatòriament
            comutils.read_string(3);

            // Enviem un result
            comutils.write_byte(6);
            comutils.write_int32(11111);
            comutils.write_string("---");
            comutils.write_byte(0);

            System.out.println("Closing server...");
            socket.close();
            System.out.println("Socket has been closed");
        }
    }

    class ServerErrorPlaying extends TestingServer {

        public ServerErrorPlaying(int port) {
            super(port);
        }

        @Override
        protected void executeTest(ComUtils comutils) throws IOException{
                // Rebem el hello
                assertEquals(1, comutils.read_byte());
                assertEquals(0, comutils.read_int32());
                assertEquals("JohannesTheGreat444", comutils.read_variable_string());

                // Enviem un ready
                comutils.write_byte(2);
                comutils.write_int32(11111);

                System.out.println("Ready Sent");

                // Rebem un play
                assertEquals(3, comutils.read_byte());
                assertEquals(11111, comutils.read_int32());

                System.out.println("Play Received");

                // Enviem un admit
                comutils.write_byte(4);
                comutils.write_int32(11111);
                comutils.write_byte(1);

                System.out.println("Approved Admit Sent");

                for (int i = 0; i < 10; i++) {
                    // Rebem un action
                    assertEquals(5, comutils.read_byte());
                    assertEquals(11111, comutils.read_int32());
                    // No puc fer un assert aquí ja que el client mou aleatòriament
                    comutils.read_string(3);

                    // Enviem un action
                    comutils.write_byte(5);
                    comutils.write_int32(11111);
                    String row = Integer.toString(i % 3);
                    String col = Integer.toString((i / 3) % 3);
                    String position = row + "-" + col;
                    comutils.write_string(position);

                    // Rebem un action
                    assertEquals(5, comutils.read_byte());
                    assertEquals(11111, comutils.read_int32());
                    // No puc fer un assert aquí ja que el client mou aleatòriament
                    comutils.read_string(3);

                    // Enviem un error
                    comutils.write_byte(8);
                    comutils.write_int32(11111);
                    comutils.write_byte(1);
                    comutils.write_variable_string("This is an error message.");
                }

                // Rebem un action
                assertEquals(5, comutils.read_byte());
                assertEquals(11111, comutils.read_int32());
                // No puc fer un assert aquí ja que el client mou aleatòriament
                comutils.read_string(3);

                // Enviem un result
                comutils.write_byte(6);
                comutils.write_int32(11111);
                comutils.write_string("1-1");
                comutils.write_byte(1);

                System.out.println("Closing server...");
                socket.close();
                System.out.println("Socket has been closed");
        }
    }

    @Test
    public void hello_test() {
        System.out.println("\nTesting hello: ");
        try {
            (new ServerHello(8181)).start();

            Client client = new Client("localhost", 8181);
            ClientProtocol protocol = new ClientProtocol(client.getComutils());
            System.out.println("Connection started...");
            // Send a HELLO message
            protocol.sendHello(29986, "GiuseppeCardlos55");
            int opCode = client.getComutils().read_byte();
            int sessionId = client.getComutils().read_int32();
            String playerName = client.getComutils().read_variable_string();
            assertEquals(1, opCode);
            assertEquals(29986, sessionId);
            assertEquals("GiuseppeCardlos55", playerName);
            client.getSocket().close();
            System.out.println("Connection closed\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void action_test() {
        System.out.println("\nTesting action: ");
        try {
            (new ServerAction(8182)).start();

            Client client = new Client("localhost", 8182);
            ClientProtocol protocol = new ClientProtocol(client.getComutils());
            System.out.println("Connection started...");
            // Send an ACTION message
            protocol.sendAction(23521, "0-1");
            int opCode = client.getComutils().read_byte();
            int sessionId = client.getComutils().read_int32();
            String position = client.getComutils().read_string(3);
            assertEquals(5, opCode);
            assertEquals(23521, sessionId);
            assertEquals("0-1", position);
            client.getSocket().close();
            System.out.println("Connection closed\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void error_test() {
        System.out.println("Testing error: ");
        try {
            (new ServerError(8183)).start();

            Client client = new Client("localhost", 8183);
            ClientProtocol protocol = new ClientProtocol(client.getComutils());
            System.out.println("Connection started...");
            // Send an ERROR message
            protocol.sendError(23521, ErrorType.IDSESSIO_INVALID);
            int opCode = client.getComutils().read_byte();
            int sessionId = client.getComutils().read_int32();
            int errorCode = client.getComutils().read_byte();
            String errorMessage = client.getComutils().read_variable_string();
            assertEquals(8, opCode);
            assertEquals(23521, sessionId);
            assertEquals(3, errorCode);
            assertEquals("ID Sessio Incorrecte", errorMessage);
            client.getSocket().close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void play_test() {
        System.out.println("\nTesting play: ");
        try {
            (new ServerPlay(8184)).start();

            Client client = new Client("localhost", 8184);
            ClientProtocol protocol = new ClientProtocol(client.getComutils());
            System.out.println("Connection started...");
            ComUtils comUtils = client.getComutils();
            // Send a PLAY message
            protocol.sendPlay(23532);
            assertEquals(3, comUtils.read_byte());
            assertEquals(23532, comUtils.read_int32());
            client.getSocket().close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void client_up_to_denied_admit_test() {
        System.out.println("\nTesting denied admit: ");
        (new ServerDeniedAdmit(8185)).start();

        Client client = new Client("localhost", 8185);
        ClientProtocol protocol = new ClientProtocol(client.getComutils());
        GameClientTest gameClient = new GameClientTest(protocol, client.getSocket());
        gameClient.startGame();
        try {
            client.getSocket().close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void client_game_test() {
        System.out.println("\nTesting whole game: ");
        (new ServerFullGame(8186)).start();

        Client client = new Client("localhost", 8186);
        ClientProtocol protocol = new ClientProtocol(client.getComutils());
        GameClientTest gameClient = new GameClientTest(protocol, client.getSocket());
        gameClient.startGame();
        try {
            client.getSocket().close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void client_error_play() {
        System.out.println("\nTesting whole game with errors sent during play: ");
        (new ServerErrorPlaying(8187)).start();

        Client client = new Client("localhost", 8187);
        ClientProtocol protocol = new ClientProtocol(client.getComutils());
        GameClientTest gameClient = new GameClientTest(protocol, client.getSocket());
        gameClient.startGame();
        try {
            client.getSocket().close();
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
