/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.client.producer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.rocketmq.client.common.ClientErrorCode;
import org.apache.rocketmq.client.exception.RequestTimeoutException;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.logging.InternalLogger;

/*
经典调用模式，这种模式我在极客时间的消息队列课程上的 rpc 设计上看到过。
通过 CompletableFuture 保存正在执行中的请求，通过 uuid 做映射。值得学习。
 */
public class RequestFutureTable {
    private static InternalLogger log = ClientLogger.getLog();
    private static ConcurrentHashMap<String, RequestResponseFuture> requestFutureTable = new ConcurrentHashMap<>();
    private static final AtomicInteger PRODUCER_NUM = new AtomicInteger(0);

    public static ConcurrentHashMap<String, RequestResponseFuture> getRequestFutureTable() {
        return requestFutureTable;
    }

    public static void scanExpiredRequest() {
        final List<RequestResponseFuture> rfList = new LinkedList<>();
        Iterator<Map.Entry<String, RequestResponseFuture>> it = requestFutureTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, RequestResponseFuture> next = it.next();
            RequestResponseFuture rep = next.getValue();

            if (rep.isTimeout()) {
                it.remove();
                rfList.add(rep);
                log.warn("remove timeout request, CorrelationId={}" + rep.getCorrelationId());
            }
        }

        for (RequestResponseFuture rf : rfList) {
            try {
                Throwable cause = new RequestTimeoutException(ClientErrorCode.REQUEST_TIMEOUT_EXCEPTION, "request timeout, no reply message.");
                rf.setCause(cause);
                rf.executeRequestCallback();
            } catch (Throwable e) {
                log.warn("scanResponseTable, operationComplete Exception", e);
            }
        }
    }

    public static AtomicInteger getProducerNum() {
        return PRODUCER_NUM;
    }
}
