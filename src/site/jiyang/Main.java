package site.jiyang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Create by StefanJi in 2020-01-21
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Must pass class file path.");
        }
        String path = args[0];
        System.out.println(String.format("========== Start Parse %s =========", path));
        try {
            FileInputStream fis = new FileInputStream(new File(path));
            int count = fis.available();
            byte[] buff = new byte[count];
            int read = fis.read(buff);
            if (read != count) {
                return;
            }
            new BytecodeParser().parse(buff);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
