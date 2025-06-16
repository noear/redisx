package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
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
    public void subscribe(BiConsumer<String, String> subscriber, String... topics) {
        subscribe(new JedisPubSub() {
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                subscriber.accept(channel, message);
            }
        }, topics);
    }

    public void subscribe(JedisPubSub subscriber, String... topics) {
        client.open(s -> {
            s.subscribe(subscriber, topics);
        });
    }

    public CompletableFuture<Thread> subscribeFuture(BiConsumer<String, String> subscriber, String... topics) {
        return subscribeFuture(new JedisPubSub() {
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                subscriber.accept(channel, message);
            }
        }, topics);
    }

    public CompletableFuture<Thread> subscribeFuture(JedisPubSub subscriber, String... topics) {
        CompletableFuture<Thread> future = new CompletableFuture();

        Thread thread = new Thread(() -> {
            try {
                subscribe(subscriber, topics);
                future.complete(Thread.currentThread());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        thread.start();

        return future;
    }

    /**
     * 根据匹配模式patterns进行订阅
     * 常用于redis事件监听
     */
    public void psubscribe(BiConsumer<String, String> subscriber, String... patterns) {
        psubscribe(new JedisPubSub() {
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                subscriber.accept(channel, message);
            }
        },  patterns);
    }

    public void psubscribe(JedisPubSub subscriber, String... patterns) {
        client.open(s -> {
            s.jedis().psubscribe(subscriber, patterns);
        });
    }

    public CompletableFuture<Thread> psubscribeFuture(BiConsumer<String, String> subscriber, String... patterns) {
        return psubscribeFuture(new JedisPubSub() {
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                subscriber.accept(channel, message);
            }
        }, patterns);
    }

    public CompletableFuture<Thread> psubscribeFuture(JedisPubSub subscriber, String... patterns) {
        CompletableFuture<Thread> future = new CompletableFuture();

        Thread thread = new Thread(() -> {
            try {
                psubscribe(subscriber, patterns);
                future.complete(Thread.currentThread());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        thread.start();

        return future;
    }

    /**
     * 发布
     */
    public void publish(String topic, String message) {
        client.open(s -> s.publish(topic, message));
    }
}
