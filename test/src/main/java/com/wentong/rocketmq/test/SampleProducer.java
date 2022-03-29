package com.wentong.rocketmq.test;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;

/**
 * 用来测试发送消息的 Producer
 */
public class SampleProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("syn1c-group-name");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        for (int i = 0; i < 10; i++) {
            Message message = new Message("TestPull", "Normal", ("Hello World" + i).getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(message);
            System.out.println(result);
        }

        for (int i = 0; i < 10; i++) {
            Message message = new Message("TestPull", "gray", ("Hello World" + i).getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(message);
            System.out.println(result);
        }
    }

}
