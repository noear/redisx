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
     * */
    public void subscribe(BiConsumer<String, String> consumer, String... topics) {
        client.open(session -> {
            session.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    consumer.accept(channel, message);
                }
            }, topics);
        });
    }

    /**
     * 发布
     * */
    public void publish(String topic, String message) {
        client.open(session -> {
            session.publish(topic, message);
        });
    }
}
