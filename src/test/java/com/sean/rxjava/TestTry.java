package com.sean.rxjava;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by guozhenbin on 2017/5/18.
 */
public class TestTry {

    public static void main(String[] args) {

        try(TestResource resource = new TestResource()){
            resource.print();
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("end");
    }

}

class TestResource implements Closeable{

    @Override
    public void close() throws IOException {
        System.out.println("close resource");
        throw new IOException("exception");
    }

    public void print(){
        System.out.println("resource process");
    }
}
