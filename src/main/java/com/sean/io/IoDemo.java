package com.sean.io;

import java.io.*;

/**
 * Created by guozhenbin on 2017/5/26.
 */
public class IoDemo {

    public static void main(String[] args) {

        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream("src/main/resources/test.txt"));
            byte[] buf = new byte[10];
            int bytesRead = in.read(buf);
            System.out.println(bytesRead);
            while (bytesRead != -1) {
                System.out.println("loop:"+bytesRead);
                for (int i = 0; i < bytesRead; i++)
                    System.out.print((char) buf[i]);
                bytesRead = in.read(buf);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
