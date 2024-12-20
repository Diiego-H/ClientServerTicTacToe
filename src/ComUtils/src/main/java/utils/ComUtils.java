package utils;

import java.io.*;

public class ComUtils {

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public ComUtils(InputStream inputStream, OutputStream outputStream) throws IOException {
        dataInputStream = new DataInputStream(inputStream);
        dataOutputStream = new DataOutputStream(outputStream);
    }

    public int read_byte() throws IOException {
        return read_bytes(1)[0] & 0xFF;
    }

    public void write_byte(int number) throws IOException {
        dataOutputStream.writeByte(number & 0xFF);
    }

    public int read_int32() throws IOException {
        byte bytes[] = read_bytes(4);

        return bytesToInt32(bytes, Endianness.BIG_ENNDIAN);
    }

    public void write_int32(int number) throws IOException {
        byte bytes[] = int32ToBytes(number, Endianness.BIG_ENNDIAN);

        dataOutputStream.write(bytes, 0, 4);
    }

    public String read_variable_string() throws IOException {
        String result = "";
        boolean one_zero_read = false;
        char c;
        
        // Read chars until a double zero is reached.
        while (true) {
            c = read_char();
            result += c;
            if (c == 0x00) {
                if (one_zero_read) {
                    // This is the second zero, string has been read.
                    return result.substring(0, result.length() - 2);
                } else {
                    one_zero_read = true;
                }
            } else {
                one_zero_read = false;
            }
        }
    }

    public String read_string(int size) throws IOException {
        String result;
        byte[] bStr = new byte[size];
        char[] cStr = new char[size];

        bStr = read_bytes(size);

        for (int i = 0; i < size; i++)
            cStr[i] = (char) bStr[i];

        result = String.valueOf(cStr);

        return result.trim();
    }

    public void write_variable_string(String str) throws IOException {
        this.write_string(str);

        byte bytes[] = {0x00, 0x00};
        dataOutputStream.write(bytes, 0, 2);
    }

    public void write_string(String str) throws IOException {
        int size = str.length();
        byte bStr[] = new byte[size];
        for (int i = 0; i < size; i++)
            bStr[i] = (byte) str.charAt(i);

        dataOutputStream.write(bStr, 0, size);
    }

    public char read_char() throws IOException {
        // Byte a char
        byte[] bChar = new byte[1];
        bChar = read_bytes(1);

        // Passem a char (cada caràcter és un byte)
        char c = (char) bChar[0];

        return c;
    }

    public void write_char(char c) throws IOException {
        // Char a byte.
        byte bChar[] = new byte[1];
        bChar[0] = (byte) c;

        // Escrivim un únic byte.
        dataOutputStream.write(bChar, 0, 1);
    }

    private byte[] int32ToBytes(int number, Endianness endianness) {
        byte[] bytes = new byte[4];

        if (Endianness.BIG_ENNDIAN == endianness) {
            bytes[0] = (byte) ((number >> 24) & 0xFF);
            bytes[1] = (byte) ((number >> 16) & 0xFF);
            bytes[2] = (byte) ((number >> 8) & 0xFF);
            bytes[3] = (byte) (number & 0xFF);
        } else {
            bytes[0] = (byte) (number & 0xFF);
            bytes[1] = (byte) ((number >> 8) & 0xFF);
            bytes[2] = (byte) ((number >> 16) & 0xFF);
            bytes[3] = (byte) ((number >> 24) & 0xFF);
        }
        return bytes;
    }

    /* Passar de bytes a enters */
    private int bytesToInt32(byte bytes[], Endianness endianness) {
        int number;

        if (Endianness.BIG_ENNDIAN == endianness) {
            number = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) |
                    ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } else {
            number = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) |
                    ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
        }
        return number;
    }

    // Llegir bytes.
    private byte[] read_bytes(int numBytes) throws IOException {
        int len = 0;
        byte bStr[] = new byte[numBytes];
        int bytesread = 0;
        do {
            bytesread = dataInputStream.read(bStr, len, numBytes - len);
            if (bytesread == -1)
                throw new IOException("Broken Pipe");
            len += bytesread;
        } while (len < numBytes);
        return bStr;
    }

    public enum Endianness {
        BIG_ENNDIAN,
        LITTLE_ENDIAN
    }
}
