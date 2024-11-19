//package com.wentong.rocketmq.transaction;
//
//import com.oracle.jrockit.jfr.Producer;
//
//public class TransactionTest {
//
//    public static void main(String[] args) {
//        ClientServiceProvider provider = new ClientServiceProvider();
//        MessageBuilder messageBuilder = new MessageBuilderImpl();
//        //构造事务生产者：事务消息需要生产者构建一个事务检查器，用于检查确认异常半事务的中间状态。
//        Producer producer = provider.newProducerBuilder()
//                .setTransactionChecker(messageView -> {
//                    /**
//                     * 事务检查器一般是根据业务的ID去检查本地事务是否正确提交还是回滚，此处以订单ID属性为例。
//                     * 在订单表找到了这个订单，说明本地事务插入订单的操作已经正确提交；如果订单表没有订单，说明本地事务已经回滚。
//                     */
//                    final String orderId = messageView.getProperties().get("OrderId");
//                    if (Strings.isNullOrEmpty(orderId)) {
//                        // 错误的消息，直接返回Rollback。
//                        return TransactionResolution.ROLLBACK;
//                    }
//                    return checkOrderById(orderId) ? TransactionResolution.COMMIT : TransactionResolution.ROLLBACK;
//                })
//                .build();
//    }
//
//}
