package p1.server;

public class ServerException extends Exception {
    public ServerException(String errMsg) {
        super("Exception in GameHandler: " + errMsg);
    }
}