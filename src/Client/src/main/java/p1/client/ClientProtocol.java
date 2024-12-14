package p1.client;

import java.io.IOException;

import p1.client.Trames.ErrorTrama;
import p1.client.Trames.ResultTrama;
import utils.ComUtils;

public class ClientProtocol {
    private ComUtils comutils;

    public ClientProtocol(ComUtils comutils) {
        this.comutils = comutils;
    }

    private void sendOpcode(MessageType type) throws IOException {
        comutils.write_byte(type.opcode);
    }

    private void sendSessionId(int sessionId) throws IOException {
        comutils.write_int32(sessionId);
    }

    public void sendHello(int sessionId, String playerName) throws IOException {
        // Write the OpCode
        sendOpcode(MessageType.HELLO);
        sendSessionId(sessionId);

        // Write a string of variable length followed by two zero bytes.
        comutils.write_variable_string(playerName);
    }

    public void sendPlay(int sessionId) throws IOException {
        // Write the OpCode
        sendOpcode(MessageType.PLAY);
        sendSessionId(sessionId);
    }

    public void sendAction(int sessionId, String position) throws IOException {
        // Write the OpCode
        sendOpcode(MessageType.ACTION);
        sendSessionId(sessionId);
        comutils.write_string(position);
    }

    public void sendError(int sessionId, ErrorType type) throws IOException {
        // Write the OpCode
        sendOpcode(MessageType.ERROR);
        sendSessionId(sessionId);
        comutils.write_byte(type.errCode);
        comutils.write_variable_string(type.errMsg);
    }

    public int readOpCode() throws IOException {
        return comutils.read_byte();
    }

    public int readReady() throws IOException {
        return comutils.read_int32();
    }

    public ErrorTrama readError() throws IOException {
        int sessionId = comutils.read_int32();
        int errorCode = comutils.read_byte();
        String errorMessage = comutils.read_variable_string();

        return new ErrorTrama(sessionId, errorCode, errorMessage);
    }

    public int readAdmit() throws IOException {
        // Descartem el sessionId i retornem el flag d'admissio
        comutils.read_int32();
        return comutils.read_byte();
    }

    public String readAction() throws IOException {
        // Descartem el sessionId i retornem el moviment fet
        comutils.read_int32();
        return comutils.read_string(3);
    }

    public ResultTrama readResult() throws IOException {
        return new ResultTrama(comutils.read_int32(), comutils.read_string(3), comutils.read_byte());
    }

    public void readAny(int opcode) throws IOException {
        // Llegim el missatge no esperat per netejar l'input.
        if (opcode == MessageType.READY.opcode) {
            readReady();
        } else if (opcode == MessageType.ADMIT.opcode) {
            readAdmit();
        } else if (opcode == MessageType.ACTION.opcode) {
            readAction();
        } else if (opcode == MessageType.RESULT.opcode) {
            readResult();
        } else if (opcode == MessageType.ERROR.opcode) {
            readError();
        }
    }

    public enum MessageType {
        HELLO(1),
        READY(2),
        PLAY(3),
        ADMIT(4),
        ACTION(5),
        RESULT(6),
        ERROR(8);

        private final int opcode;

        MessageType(final int opcode) {
            this.opcode = opcode;
        }

        public int getValue() {
            return opcode;
        }
    }

    public enum ErrorType {
        MOV_DESCONEGUT(0, "Moviment Desconegut"),
        MOV_INVALID(1, "Moviment Invalid"),
        COMANDA_INESPERADA(2, "Comanda Inesperada"),
        IDSESSIO_INVALID(3, "ID Sessio Incorrecte");

        private final int errCode;
        private final String errMsg;

        ErrorType(final int errCode, final String errMsg) {
            this.errCode = errCode;
            this.errMsg = errMsg;
        }
    }
}
