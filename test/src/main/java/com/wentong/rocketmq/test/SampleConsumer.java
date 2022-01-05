package com.wentong.rocketmq.test;

import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用来测试接收消息的消费者
 */
public class SampleConsumer {

    public static void main(String[] args) throws Exception {
        DefaultLitePullConsumer consumer = new DefaultLitePullConsumer("litepull");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.setAutoCommit(true);
        consumer.subscribe("TestPull", "*");
//        consumer.registerTopicMessageQueueChangeListener(new TopicMessageQueueChangeListener(){
//
//            @Override
//            public void onChanged(String topic, Set<MessageQueue> messageQueues) {
//
//            }
//        });
        consumer.start();
        while (true) {
            List<MessageExt> messages = consumer.poll(1000);
            for (MessageExt message : messages) {
                System.out.println(message);
            }
            TimeUnit.SECONDS.sleep(1);
        }
//        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("basic-push");
//        consumer.setNamesrvAddr("localhost:9876");
//        consumer.subscribe("TopicTest1", "*");
//        consumer.registerMessageListener((MessageListenerConcurrently) (list, consumeConcurrentlyContext) -> {
//            System.out.println(Thread.currentThread().getName());
//            System.out.println(list);
//            System.out.println("----------");
//            System.out.println(consumeConcurrentlyContext);
//            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
//        });
//        consumer.start();
    }

}
