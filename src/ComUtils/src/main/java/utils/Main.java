package utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello world");

        // Test code
        File file = new File("test.dad");
        try {
            file.createNewFile();
            ComUtilsService comUtilsService = new ComUtilsService(new FileInputStream(file),
                    new FileOutputStream(file));
            comUtilsService.writeTest();
            System.out.println(comUtilsService.readTest());
        } catch (IOException e) {
            System.out.println("Error Found during Operation:" + e.getMessage());
            e.printStackTrace();
        }
    }
}