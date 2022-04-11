package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

import java.util.Collection;
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
        client.open(s -> s.key(queueName).persist().listAdd(item));
    }


    /**
     * 添加全部
     * */
    public void addAll(Collection<String> items) {
        client.open(s -> {
            s.key(queueName).persist().listAddRange(items);
        });
    }


    /**
     * 推出
     * */
    public String pop() {
        return client.openAndGet(s -> s.key(queueName).listPop());
    }

    /**
     * 推出更多
     * */
    public void popAll(Consumer<String> consumer) {
        client.open(s -> {
            s.key(queueName);

            while (true) {
                String item = s.listPop();
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
        return client.openAndGet(s -> s.key(queueName).listPeek());
    }


    public long size() {
        return client.openAndGet(s -> s.key(queueName).listLen());
    }

    public void clear() {
        client.open(s -> s.key(queueName).delete());
    }
}
