package com.wentong.rocketmq.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestQueueLock {

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            Object locker = LockObject.getLock(String.valueOf(i));
            synchronized (locker) {
                System.out.println("Thread:" + Thread.currentThread().getName());
            }
        }
    }

    static class LockObject {
        static Map<String, Object> map = new ConcurrentHashMap<>();

        public static Object getLock(String key) {
            Object locker = map.get(key);
            if (locker == null) {
                Object value = new Object();
                Object prev = map.putIfAbsent(key, value);
                if (prev != null) {
                    return prev;
                } else {
                    return value;
                }
            } else {
                return locker;
            }
        }
    }
}
