package com.wentong.rocketmq.util;

import java.util.concurrent.TimeUnit;

public final class UtilAll {

    private UtilAll(){}

    public static void sleepMillSeconds(int n) {
        try {
            TimeUnit.MILLISECONDS.sleep(n);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

}
