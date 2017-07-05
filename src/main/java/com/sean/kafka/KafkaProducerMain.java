package com.sean.kafka;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.util.Properties;

/**
 * Created by guozhenbin on 2017/4/25.
 */
public class KafkaProducerMain {

//    private static final String KAFKA_BROKER_LIST = "10.86.51.130:9092,10.86.51.129:9092";
    public static final String KAFKA_BROKER_LIST = "192.168.235.151:9092";

    public static void main(String[] args) throws Exception {
        testV802();
//        testV101();
//        testPerfonmance();
       /* String str = "{\"date\":\"2017-04-25 17:49:50\",\"state\":\"202\",\"location_msg\":\"{\"command_id\":0,\"gid\":\"093223A6-319D-E072-2DAC-B34258455756\",\"lat\":\"29.40513577004284\",\"lon\":\"105.58789588618953\",\"pid\":\"10010\",\"traceId\":\"14931137896kr4j99n2g4_qpub_locationUpload\",\"uid\":\"A24E8290-9940-4EBB-8B2A-D2639BC82395\",\"uname\":\"bfacrgf8643\",\"uuid\":\"s_ERHNEIVNE6S445MUVY65M6WPB4\",\"vid\":\"80011128\"}\",\"buType\":\"SIGHT\"}";
        System.out.println(str.getBytes().length);*/
    }

   /* public static void testPerfonmance() throws Exception {

        String[] args = {"test", "10000", "10", "1000",
                "bootstrap.servers=10.86.51.130:9092,10.86.51.129:9092",
                "batch.size=500",
                "record.size=5000",
                "key.serializer=kafka.serializer.StringEncoder"};

        ProducerPerformance.main(args);
    }
*/
    /*public static void testV801() throws IOException {
        //limit 1000
        //bm_msg:10000 bm_ms:3000 sendTime:957
        //bm_msg:10000 bm_ms:3000 ack:1 sendTime:1091
        //bm_msg:1000 bm_ms:3000 sendTime:1964
        //bm_msg:10000 bm_ms:3000 bnm:500  sendTime:1038

        //limit 5000
        //bm_msg:10000 bm_ms:3000 sendTime:1797

        //limit 10000
        //bm_msg:10000 bm_ms:3000 sendTime:3364
        //ack:1 4110
        //ack:0 4470

        //batch 100  totalTime:5207 sendTime:5116

        //batch 1000 totalTime:3352 sendTime:3235
        //batch 3000 totalTime:3468 sendTime:3371
        //batch 5000  totalTime:4382 sendTime:4258
        RateLimiter rateLimiter = RateLimiter.create(10000);
        Properties props = new Properties();
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("metadata.broker.list", KAFKA_BROKER_LIST);
        props.put("producer.type", "async");
        props.put("queue.buffering.max.ms", "1000");
        props.put("queue.buffering.max.messages", "10000");
        props.put("batch.num.messages", "3000");
        props.put("compression.codec", "snappy");
//        props.put("compression.codec", "gzip");
        props.put("request.required.acks", "-1");

        ProducerConfig producerConfig = new ProducerConfig(props);
        Producer producer = new Producer(producerConfig);
        int i = 0;
        double totalSlept = 0;
        long start = System.currentTimeMillis();
        long sendTime = 0;
        do {
//            double time = rateLimiter.acquire();
//            totalSlept += time;
            String topic = "test";
            String key = "key";
            String content = "message" + i;
            KeyedMessage message = new KeyedMessage(topic, key, content);
            long sendStart = System.currentTimeMillis();
            producer.send(message);

            long sendCost = System.currentTimeMillis() - sendStart;
            if (sendCost > 0) {
                System.out.println(String.format("%d:%d", i, sendCost));
            }
            sendTime += sendCost;
        } while (i++ < 50000);
        System.out.println("totalTime:" + (System.currentTimeMillis() - start));
        System.out.println("sendTime:" + sendTime);
//        System.out.println("totalSlept:"+totalSlept);
    }*/

    public static void testV802(){
        Properties props = new Properties();
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("metadata.broker.list", KAFKA_BROKER_LIST);
        props.put("producer.type", "async");
        props.put("queue.buffering.max.ms", "1000");
        props.put("quearrcityue.buffering.max.messages", "10000");
        props.put("batch.num.messages", "3000");
        props.put("compression.codec", "snappy");
//        props.put("compression.codec", "gzip");
        props.put("request.required.acks", "-1");

        ProducerConfig producerConfig = new ProducerConfig(props);
        Producer producer = new Producer(producerConfig);
        int i = 0;
        long start = System.currentTimeMillis();
        long sendTime = 0;
        do {
//            double time = rateLimiter.acquire();
//            totalSlept += time;
            String topic = "test";
            String key = "key";
            String content = "message" + i;
            KeyedMessage message = new KeyedMessage(topic, key, content);
            long sendStart = System.currentTimeMillis();
            producer.send(message);

            long sendCost = System.currentTimeMillis() - sendStart;
            if (sendCost > 0) {
                System.out.println(String.format("%d:%d", i, sendCost));
            }
            sendTime += sendCost;
        } while (i++ < 50000);
        System.out.println("totalTime:" + (System.currentTimeMillis() - start));
        System.out.println("sendTime:" + sendTime);
    }


}
