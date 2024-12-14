package p1.server.ai;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;

import p1.server.Server;

public class ServerAI extends Server {

    public ServerAI(int port) {
        super(port);
    }

    @Override
    public void init() {
        while (true) {
            try {
                Socket socket = ss.accept();
                (new Thread((new GameHandlerAI(socket)))).start();
            } catch (IOException e) {
                throw new RuntimeException("I/O error when accepting a client:\n" + e.getMessage());
            } catch (SecurityException e) {
                throw new RuntimeException("Operation not accepted:\n" + e.getMessage());
            } catch (IllegalBlockingModeException e) {
                throw new RuntimeException("There is no connection ready to be accepted:\n" + e.getMessage());
            }
        }
    }
}
