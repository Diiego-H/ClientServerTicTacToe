package p1.client.Trames;

public class ErrorTrama {
    private int sessionId;
    private int errorCode;
    private String errorMessage;

    public ErrorTrama(int sessionId, int errorCode, String errorMessage) {
        this.sessionId = sessionId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
