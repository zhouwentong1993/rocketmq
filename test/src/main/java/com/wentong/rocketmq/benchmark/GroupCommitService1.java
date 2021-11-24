package com.wentong.rocketmq.benchmark;

import com.wentong.rocketmq.util.UtilAll;
import org.apache.rocketmq.common.ServiceThread;
import org.apache.rocketmq.store.CommitLog;
import org.apache.rocketmq.store.PutMessageSpinLock;
import org.apache.rocketmq.store.PutMessageStatus;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

/**
 * 在写入并发越高的情况下，双链表的效果越好。
 * 10w 写入，100 线程
 * total time is: 144721 非双链表
 * total time is: 132833 双链表
 * 1w 写入，100 线程
 * total time is: 144721 非双链表
 * total time is: 132833 双链表
 */
public class GroupCommitService1 extends ServiceThread {

    public static void main(String[] args) throws Exception {
        GroupCommitService1 groupCommitService1 = new GroupCommitService1();
        groupCommitService1.start();
        long start = System.currentTimeMillis();
        int requestCount = 100000;
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < requestCount / threadCount; j++) {
                    CommitLog.GroupCommitRequest request = new CommitLog.GroupCommitRequest(10, 10);
                    groupCommitService1.putRequest(request);
                    try {
                        request.future().get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                latch.countDown();
                System.out.println("Thread: " + Thread.currentThread().getName() + " executed job ok!");
            }, "putThread" + i).start();

        }
        latch.await();
        System.out.println("total time is: " + (System.currentTimeMillis() - start));
    }

    private volatile LinkedList<CommitLog.GroupCommitRequest> requestsWrite = new LinkedList<>();
        private volatile LinkedList<CommitLog.GroupCommitRequest> requestsRead = new LinkedList<>();
    private final PutMessageSpinLock lock = new PutMessageSpinLock();

    public synchronized void putRequest(final CommitLog.GroupCommitRequest request) {
        lock.lock();
        try {
            this.requestsWrite.add(request);
        } finally {
            lock.unlock();
        }
        this.wakeup();
    }

    /**
     * 交换
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
    private void doCommit() {
        if (!this.requestsRead.isEmpty()) {
            for (CommitLog.GroupCommitRequest req : this.requestsRead) {
                // 模拟一毫秒写入时间
                UtilAll.sleepMillSeconds(1);
                req.wakeupCustomer(PutMessageStatus.PUT_OK);
            }
            this.requestsRead = new LinkedList<>();
        } else {
            // Because of individual messages is set to not sync flush, it
            // will come to this process
            // 模拟一毫秒写入时间
            UtilAll.sleepMillSeconds(1);
        }
    }

    @Override
    public void run() {

        while (!this.isStopped()) {
            try {
                this.waitForRunning(10);
                // 每隔 10ms 同步一次。
                this.doCommit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onWaitEnd() {
        swapRequests();
    }

    @Override
    public String getServiceName() {
        return "GroupCommitService1";
    }

    @Override
    public long getJointime() {
        return 1000 * 60 * 5L;
    }
}