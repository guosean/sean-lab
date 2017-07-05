package com.sean.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by guozhenbin on 2017/5/26.
 */
public class NioDemo {

    public static void main(String[] args) {

        try (FileInputStream inputStream = new FileInputStream("src/main/resources/test.txt")) {
            FileChannel channel = inputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1);
            int readBytes = channel.read(buffer);
            while (readBytes != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    System.out.print((char) buffer.get());
                }
                buffer.compact();
                readBytes = channel.read(buffer);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
