import org.junit.Test;
import static org.junit.Assert.*;
import utils.ComUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ComUtilsTest {

    @Test
    public void example_test() {
        File file = new File("test");
        try {
            file.createNewFile();
            ComUtils comUtils = new ComUtils(new FileInputStream(file), new FileOutputStream(file));
            comUtils.write_int32(2);
            int readedInt = comUtils.read_int32();

            assertEquals(2, readedInt);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void write_read_byte() {
        File file = new File("test_byte");
        try {
            file.createNewFile();
            ComUtils comUtils = new ComUtils(new FileInputStream(file), new FileOutputStream(file));
            comUtils.write_byte(12);
            comUtils.write_byte(0xFF);
            comUtils.write_byte(0xAED);
            comUtils.write_byte(1);
            comUtils.write_byte(0);
            assertEquals(12, comUtils.read_byte());
            assertEquals(0xFF, comUtils.read_byte());
            assertEquals(0xED, comUtils.read_byte());
            assertEquals(1, comUtils.read_byte());
            assertEquals(0, comUtils.read_byte());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void write_read_char_test() {
        File file = new File("test_write_read_char");
        try {
            file.createNewFile();
            ComUtils comUtils = new ComUtils(new FileInputStream(file), new FileOutputStream(file));
            comUtils.write_char('o');
            comUtils.write_char('q');
            comUtils.write_char('-');
            comUtils.write_char('_');
            comUtils.write_char('D');

            assertEquals('o', comUtils.read_char());
            assertEquals('q', comUtils.read_char());
            assertEquals('-', comUtils.read_char());
            assertEquals('_', comUtils.read_char());
            assertEquals('D', comUtils.read_char());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void read_variable_string_test() {
        File file = new File("test_read_variable_string");
        try {
            file.createNewFile();
            ComUtils comUtils = new ComUtils(new FileInputStream(file), new FileOutputStream(file));
            comUtils.write_variable_string("Asiguela");
            comUtils.write_variable_string("alks0djfhlasmdncioahejcb0auewirh8364r8yuihrf0jhgf6ewygcbahus0cgkhjdsbctyt7we6gcas");
            comUtils.write_variable_string("Santiago de compostela00fue un herorelorelore");
            comUtils.write_variable_string("Una 0caja dentro de una caja que esta dentro de otra caja \n");
            comUtils.write_variable_string("RecklessPolynomial");

            assertEquals("Asiguela", comUtils.read_variable_string());
            assertEquals("alks0djfhlasmdncioahejcb0auewirh8364r8yuihrf0jhgf6ewygcbahus0cgkhjdsbctyt7we6gcas",
                    comUtils.read_variable_string());
            assertEquals("Santiago de compostela00fue un herorelorelore", comUtils.read_variable_string());
            assertEquals("Una 0caja dentro de una caja que esta dentro de otra caja \n",
                    comUtils.read_variable_string());
            assertEquals("RecklessPolynomial", comUtils.read_variable_string());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
