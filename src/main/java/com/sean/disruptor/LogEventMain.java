package com.sean.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by guozhenbin on 2017/4/25.
 */
public class LogEventMain {

    public static void main(String[] args) throws InterruptedException {
        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        Disruptor<LongEvent> disruptor = new Disruptor<>(LongEvent::new, bufferSize, executor);

        // Connect the handler
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            Thread.sleep(5000);
            System.out.println("Event: " + event);
        });

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();

        ByteBuffer bb = ByteBuffer.allocate(16);
        for (long l = 0; true; l++)
        {
            bb.putLong(0, l);
            long id = ringBuffer.next();
            LongEvent longEvent = ringBuffer.get(id);
            longEvent.set(l+3);
            ringBuffer.publish(id);
            System.out.println(String.format("remain:%d,id:%d,time:%d",ringBuffer.remainingCapacity(),id,System.currentTimeMillis()));
            /*ringBuffer.publishEvent((event, sequence, buffer) -> event.set(buffer.getLong(0)), bb);*/
        }

    }

}
