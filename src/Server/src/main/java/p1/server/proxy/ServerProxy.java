package p1.server.proxy;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.IllegalBlockingModeException;

import p1.server.Server;

public class ServerProxy extends Server {

    public ServerProxy(int port) {
        super(port);
    }

    @Override
    public void init() {
        while (true) {
            try {
                // Client 1
                Socket s1 = ss.accept();

                // Client 2 (no perdem la connexió de 1 si falla quelcom)
                Socket s2 = null;
                do {
                    try {
                        s2 = ss.accept();
                    } catch (IOException e) {
                        throw new RuntimeException("I/O error when accepting 2nd client:\n" + e.getMessage());
                    } catch (SecurityException e) {
                        throw new RuntimeException("Operation not accepted:\n" + e.getMessage());
                    } catch (IllegalBlockingModeException e) {
                        throw new RuntimeException("There is no connection ready to be accepted:\n" + e.getMessage());
                    }
                } while(s2 == null);

                // Nou thread gestiona ambdós clients
                (new Thread((new GameHandlerProxy(s1,s2)))).start();
            } catch (IOException e) {
                throw new RuntimeException("I/O error when accepting 1st client:\n" + e.getMessage());
            } catch (SecurityException e) {
                throw new RuntimeException("Operation not accepted:\n" + e.getMessage());
            } catch (IllegalBlockingModeException e) {
                throw new RuntimeException("There is no connection ready to be accepted:\n" + e.getMessage());
            }
        }
    }
}
