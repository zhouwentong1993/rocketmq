package com.wentong.rocketmq.benchmark;

import org.apache.rocketmq.store.CommitLog;
import org.apache.rocketmq.store.PutMessageSpinLock;

import java.util.LinkedList;

/**
 * 测试 RocketMQ 中同步刷盘中 messageExt.isWaitStoreMsgOK() == true 时的两个读写链表设计性能
 */
public class SwapRequestTest implements Runnable {

    private volatile LinkedList<CommitLog.GroupCommitRequest> requestsWrite = new LinkedList<>();
    private volatile LinkedList<CommitLog.GroupCommitRequest> requestsRead = new LinkedList<>();

    private final PutMessageSpinLock lock = new PutMessageSpinLock();

    /**
     * 放请求
     */
    public synchronized void putRequest(final CommitLog.GroupCommitRequest request) {
        lock.lock();
        try {
            this.requestsWrite.add(request);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 交换请求
     */
    private void swapRequests() {
        lock.lock();
        try {
            LinkedList<CommitLog.GroupCommitRequest> tmp = this.requestsWrite;
            this.requestsWrite = this.requestsRead;
            this.requestsRead = tmp;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        int messageCount = 1000000;

    }

    @Override
    public void run() {

    }
}
