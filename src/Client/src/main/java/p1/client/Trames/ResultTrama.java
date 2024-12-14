package p1.client.Trames;

public class ResultTrama {
    private int sessionId;
    private String moviment;
    private int flag;

    public ResultTrama(int sessionId, String moviment, int flag) {
        this.sessionId = sessionId;
        this.moviment = moviment;
        this.flag = flag;
    }

    public int getSessionId() {
        return sessionId;
    }

    public String getMoviment() {
        return moviment;
    }

    public int getFlag() {
        return flag;
    }
}
