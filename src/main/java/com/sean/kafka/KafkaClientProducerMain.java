package com.sean.kafka;

import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by guozhenbin on 2017/4/26.
 * 问题：
 * 1、callback是每条还是一批
 */
public class KafkaClientProducerMain {

    //    public static final String BROKER_0811 = "10.86.51.130:9092,10.86.51.129:9092";
//    public static final String BROKER_0822 = "192.168.235.151:9092";
    public static final String BROKER_0900 = "192.168.235.152:9092";

    public static void main(String[] args) throws Exception {
        testV920();
    }

    public static void testV920() throws IOException {
        Properties properties = new Properties();

        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER_0900);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, "1000");
        properties.put(ProducerConfig.LINGER_MS_CONFIG, "5000");
        properties.put(ProducerConfig.RETRIES_CONFIG, "1");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");


        //totalTime:3473,totalProTime:3227883,totalSendTime:3268
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG,"5000000");//5M
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,"3000");
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,"500");
        properties.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG,"3600000");
        properties.put(ProducerConfig.SEND_BUFFER_CONFIG,"");
        properties.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG,"");


        KafkaProducer producer = new KafkaProducer(properties);
        long start = System.currentTimeMillis();
        long totalTime = 0;
        long totalSendTime = 0;
        for (int i = 0; i < 100000; i++) {

            String topic = "test";
            String key = "key";
            String content = "content" + i;
            ProducerRecord record = new ProducerRecord(topic, key, content);

            Callback callback = new ProducerCallBack(System.currentTimeMillis(),content);
            long startSend = System.currentTimeMillis();
            producer.send(record, callback);
            totalSendTime += (System.currentTimeMillis() - startSend);
        }

        totalTime = System.currentTimeMillis() - start;

        System.out.println(String.format("totalTime:%d,totalProTime:%d,totalSendTime:%d,callbackTotal:%d", totalTime, ProducerCallBack.totleTime, totalSendTime,ProducerCallBack.cbTotal.get()));
        if (System.in.read() != -1) {
            System.exit(1);
        }
    }

}

class ProducerCallBack implements Callback {

    public static long totleTime = 0;
    String value;
    public static AtomicLong cbTotal = new AtomicLong(0);
    long startTime;

    ProducerCallBack(long startTime) {
        this.startTime = startTime;
    }

    ProducerCallBack(long startTime,String value){
        this(startTime);
        this.value = value;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        long timeSlt = System.currentTimeMillis() - startTime;
        totleTime += timeSlt;
        cbTotal.incrementAndGet();
//        System.out.println(String.format("time:%d,offset:%d", timeSlt, metadata.offset()));
        try {
            FileUtils.writeStringToFile(new File("/Users/guozhenbin/tmp/callback.out"),value+"\n",Charset.defaultCharset(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
