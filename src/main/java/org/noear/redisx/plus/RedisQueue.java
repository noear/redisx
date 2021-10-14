package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

import java.util.function.Consumer;

/**
 * Redis 队列
 *
 * @author noear
 * @since 1.0
 */
public class RedisQueue {
    private final RedisClient client;
    private final String queueName;

    public RedisQueue(RedisClient client, String queueName) {
        this.client = client;
        this.queueName = queueName;
    }

    /**
     * 添加
     * */
    public void add(String item) {
        client.open(session -> session.key(queueName).expire(-2).listAdd(item));
    }

    /**
     * 冒泡
     * */
    public String pop() {
        return client.openAndGet(session -> session.key(queueName).listPop());
    }

    /**
     * 冒泡更多
     * */
    public void popAll(Consumer<String> consumer) {
        client.open(session -> {
            session.key(queueName);

            while (true) {
                String item = session.listPop();
                if (item == null) {
                    break;
                } else {
                    consumer.accept(item);
                }
            }
        });
    }

    /**
     * 预览
     * */
    public String peek() {
        return client.openAndGet(session -> session.key(queueName).listPeek());
    }
}
