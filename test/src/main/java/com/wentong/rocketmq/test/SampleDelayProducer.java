package com.wentong.rocketmq.test;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class SampleDelayProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("delay-producer");
        producer.setNamesrvAddr("localhost:9876");
        producer.setSendMsgTimeout(100 * 1000);
        producer.start();
//        for (int i = 0; i < 10; i++) {
//            Message message = new Message("delay", ("hello-delay" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
//            message.setDelayTimeLevel(getDelayLevel("1h"));
//            producer.send(message);
//        }

        for (int i = 0; i < 18; i++) {
            Message message = new Message("delay", ("hello-delay" + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            message.setDelayTimeLevel(i + 1);
            producer.send(message);
        }
        producer.shutdown();
    }

    private static int getDelayLevel(String time) {
        Objects.requireNonNull(time, "time can't be null");
        String messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h";
        String[] levels = messageDelayLevel.split(" ");
        for (int i = 0; i < levels.length; i++) {
            if (time.equals(levels[i])) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException("can't find specific time:" + time);
    }

}
