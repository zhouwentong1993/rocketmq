package com.wentong.rocketmq.test;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;

public class SampleDelayConsumer {

    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("delay-push");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("delay", "*");
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            System.out.println(System.currentTimeMillis());
            System.out.println(msgs);
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        consumer.start();

    }

}
