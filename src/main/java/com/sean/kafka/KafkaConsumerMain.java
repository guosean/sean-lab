package com.sean.kafka;

import com.google.common.collect.Lists;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Iterator;
import java.util.Properties;

/**
 * Created by guozhenbin on 2017/4/27.
 */
public class KafkaConsumerMain {

    public static final String BROKER_0811 = "10.86.51.130:9092,10.86.51.129:9092";
    public static final String BROKER_0822 = "192.168.235.151:9092";
    public static final String BROKER_0900 = "192.168.235.152:9092";

    public static final String BROKER_pro = "l-qkafkapub1.ops.cn2.qunar.com:9092,l-qkafkapub2.ops.cn2.qunar.com:9092,l-qkafkapub3.ops.cn2.qunar.com:9092,l-qkafkapub4.ops.cn2.qunar.com:9092";
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put("metadata.broker.list",BROKER_0822);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,BROKER_0900);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"sean.test");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,"1000");
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,"3000");
        properties.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, RangeAssignor.class.getName());

        KafkaConsumer kafkaConsumer = new KafkaConsumer(properties);
        kafkaConsumer.subscribe(Lists.newArrayList("test"));

        while (true) {
            ConsumerRecords records =  kafkaConsumer.poll(1000);
            if(records.count() > 0){
                System.out.println(records);
            }
            Iterator<ConsumerRecord> it = records.iterator();
            while(it.hasNext()){
                System.out.println(it.next().value());
            }

            ConsumerRecords records1 = kafkaConsumer.poll(1000);


        }

    }

}
