package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ComUtilsService {
    private ComUtils comUtils;

    public ComUtilsService(InputStream inputStream, OutputStream outputStream) throws IOException {
        comUtils = new ComUtils(inputStream, outputStream);
    }

    public void writeTest() throws IOException {
        // Test data
        String name = "Diego Gabaldon Anton"; // Size 20
        int age = 30;
        String comment = "is my age.";        // Size 10
        
        // Write test
        comUtils.write_string(name);
        comUtils.write_int32(age);
        comUtils.write_string(comment);
    }

    public String readTest() throws IOException {
        String result = "";

        // Read data generated in writeTest()
        result += comUtils.read_string(20);
        result += String.valueOf(comUtils.read_int32());
        result += comUtils.read_string(10);

        return result;
    }
}
