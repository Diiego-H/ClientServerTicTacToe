package p1.server;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import utils.ComUtils;

public class ServerProtocol {
    private static final String IO_EXC_MSG = "Lost connection with client";
    private ComUtils comutils;
    private int sessionID;

    public ServerProtocol(ComUtils comutils) {
        this.comutils = comutils;
        this.sessionID = generateID();
    }

    public int getSessionID() {
        return sessionID;
    }

    private int generateID() {
        // Nombre random de 5 digits, primer no 0
        Random r = new Random();
        return ((1 + r.nextInt(9)) * 10000 + r.nextInt(10000));
    }

    public void cleanInputData(MessageType type) throws ServerException {
        // Considerem trames des de Client a Servidor (menys ERROR)
        readSessionID();
        switch (type) {
            case HELLO:
                readPlayerName();
                break;
            
            case PLAY:
                break;

            case ACTION:
                readPosition();
                break;

            default:
                System.out.println("Command impossible from client. Opcode: " + type.opcode);
        }
    }

    private int readOpcode() throws ServerException {
        try {
            return comutils.read_byte();
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    public MessageType getMessageType() throws ServerException {
        return MessageType.getMessageType(readOpcode());
    }

    private int readSessionID() throws ServerException {
        try {
            return comutils.read_int32();
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    private boolean validSessionID(int sessionID) {
        return sessionID == 0;
    }

    private boolean isSessionID() throws ServerException {
        return this.sessionID == readSessionID();
    }

    private String readPlayerName() throws ServerException {
        try {
            return comutils.read_variable_string();
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    public String receiveHello() throws ServerException {
        boolean valid = validSessionID(readSessionID());
        String nomJugador = readPlayerName();

        // Comprovacio format ID sessio
        return (valid ? nomJugador : null);
    }

    public boolean receivePlay() throws ServerException {
        return isSessionID();
    }

    private String readPosition() throws ServerException {
        try {
            return comutils.read_string(3);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    public String receiveAction() throws ServerException {
        boolean valid = isSessionID();
        String position = readPosition();
        return (valid ? position : null);
    }

    private int readErrCode() throws ServerException {
        try {
            return comutils.read_byte();
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    private String readErrMsg() throws ServerException {
        try {
            return comutils.read_variable_string();
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    public String receiveError() throws ServerException {
        boolean valid = isSessionID();
        int errCode = readErrCode(); // Not used
        String errorMsg = readErrMsg();
        return (valid ? errorMsg : null);
    }
    
    private void sendOpcode(MessageType type) throws ServerException {
        try {
            comutils.write_byte(type.opcode);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }
    
    private void sendSessionID() throws ServerException {
        try {
            comutils.write_int32(sessionID);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }
    
    public void sendReady() throws ServerException {
        sendOpcode(MessageType.READY);
        sendSessionID();
    }

    private void sendPosition(String position) throws ServerException {
        try {
            comutils.write_string(position);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    public void sendAction(String position) throws ServerException {
        sendOpcode(MessageType.ACTION);
        sendSessionID();
        sendPosition(position);
    }

    private void sendFlag(int flag) throws ServerException {
        try {
            comutils.write_byte(flag);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }
    
    public void sendAdmit(int flag) throws ServerException {
        sendOpcode(MessageType.ADMIT);
        sendSessionID();
        sendFlag(flag);
    }
    
    public void sendResult(String position, int result) throws ServerException {
        sendOpcode(MessageType.RESULT);
        sendSessionID();
        sendPosition(position);
        sendFlag(result);
    }

    private void sendErrCode(int errCode) throws ServerException {
        try {
            comutils.write_byte(errCode);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    private void sendErrMsg(String errMsg) throws ServerException {
        try {
            comutils.write_variable_string(errMsg);
        } catch (SocketTimeoutException e) {
            throw new ServerException(e.getMessage());
        } catch (IOException e) {
            throw new ServerException(IO_EXC_MSG);
        }
    }

    public void sendError(ErrorType type) throws ServerException {
        sendOpcode(MessageType.ERROR);
        sendSessionID();
        sendErrCode(type.errCode);
        sendErrMsg(type.errMsg);
    }

    public enum ErrorType {
        MOV_DESCONEGUT(0, "Moviment Desconegut"),
        MOV_INVALID(1, "Moviment Invalid"),
        COMANDA_INESPERADA(2, "Comanda Inesperada"),
        IDSESSIO_INVALID(3, "ID Sessio Incorrecte"),
        SESSIO_INCORRECTE(9, "Sessio Incorrecte");

        private final int errCode;
        private final String errMsg;
        ErrorType(final int errCode, final String errMsg) {
            this.errCode = errCode;
            this.errMsg = errMsg;
        }
    }

    public enum MessageType {
        NONE(0),
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

        private static Map<Integer, MessageType> types = new HashMap<>();
        static {
            for (MessageType t : MessageType.values()) {
                types.put(t.opcode,t);
            }
        }

        private static MessageType getMessageType(int opcode) {
            return types.containsKey(opcode) ? types.get(opcode) : NONE;
        }
    }

}