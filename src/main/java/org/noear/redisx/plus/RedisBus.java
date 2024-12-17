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

    public CompletableFuture<Thread> subscribeFuture(BiConsumer<String, String> consumer, String... topics) {
        CompletableFuture<Thread> future = new CompletableFuture();

        Thread thread = new Thread(() -> {
            try {
                subscribe(consumer, topics);
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
    public void psubscribe(BiConsumer<String, String> consumer, String... patterns) {
		client.open(s -> {
            s.jedis().psubscribe(new JedisPubSub() {
                @Override
                public void onPMessage(String pattern,String channel, String message) {
                    consumer.accept(channel, message);
                }
            }, patterns);
        });
	}
	
	public CompletableFuture<Thread> psubscribeFuture(BiConsumer<String, String> consumer, String... patterns) {
		CompletableFuture<Thread> future = new CompletableFuture();

        Thread thread = new Thread(() -> {
            try {
            	psubscribe(consumer,patterns);
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
