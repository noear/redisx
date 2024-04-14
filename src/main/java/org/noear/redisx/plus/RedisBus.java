package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import redis.clients.jedis.JedisPubSub;

import java.util.function.BiConsumer;

/**
 * Redis 总线
 *
 * @author noear
 * @since 1.0
 */
public class RedisBus {
    private final RedisClient client;

    public RedisBus(RedisClient client) {
        this.client = client;
    }

    /**
     * 订阅
     */
    public void subscribe(BiConsumer<String, String> consumer, String... topics) {
        client.open(s -> {
            s.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    consumer.accept(channel, message);
                }
            }, topics);
        });
    }

    public Thread subscribeInThread(BiConsumer<String, String> consumer, String... topics) {
        Thread thread = new Thread(() -> {
            subscribe(consumer, topics);
        });
        thread.start();

        return thread;
    }

    /**
     * 发布
     */
    public void publish(String topic, String message) {
        client.open(s -> s.publish(topic, message));
    }
}
