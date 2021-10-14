package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

/**
 * Redis 分布式锁
 *
 * @author noear
 * @since 1.0
 * */
public class RedisLock {
    private final RedisClient client;
    private final String lockName;

    public RedisLock(RedisClient client, String lockName) {
        this.client = client;
        this.lockName = lockName;
    }

    /**
     * 尝试锁
     *
     * @param inSeconds 锁定时间
     * @param holder    持有人
     */
    public boolean tryLock(int inSeconds, String holder) {
        return client.openAndGet((s) -> s.key(lockName).expire(inSeconds).lock(holder));
    }

    /**
     * 尝试锁
     *
     * @param inSeconds 锁定时间
     */
    public boolean tryLock(int inSeconds) {
        return tryLock(inSeconds, "_");
    }

    /**
     * 尝试锁 （默认为3秒）
     */
    public boolean tryLock() {
        return tryLock(3);
    }



    /**
     * 解锁
     */
    public void unLock(String holder) {
        client.open((s) -> {
            if (holder == null || holder.equals(s.key(lockName).get())) {
                s.key(lockName).delete();
            }
        });
    }

    /**
     * 解锁
     */
    public void unLock() {
        unLock(null);
    }

    /**
     * 检查是否已被锁定
     */
    public boolean isLocked() {
        return client.openAndGet((s) -> s.key(lockName).exists());
    }

    /**
     * 获取持有人
     */
    public String getHolder() {
        return client.openAndGet((s) -> s.key(lockName).get());
    }

}
