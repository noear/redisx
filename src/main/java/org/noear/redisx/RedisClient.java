package org.noear.redisx;

import org.noear.redisx.plus.*;
import org.noear.redisx.utils.TextUtil;
import redis.clients.jedis.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Redis 客户端
 *
 * @author noear
 * @since 1.0
 */
 public class RedisClient {
    private JedisPool jedisPool;
    private String keyPrefix;

    /**
     * 获取key的前缀
     * */
    public String getKeyPrefix() {
        return keyPrefix;
    }

    public RedisClient(Properties prop) {
        String db = prop.getProperty("db");

        if (TextUtil.isEmpty(db)) {
            throw new RuntimeException("RedisClient: Properties lacks the db parameter!");
        }

        initDo(prop, Integer.parseInt(db), 0);
    }

    public RedisClient(Properties prop, int db) {
        initDo(prop, db, 0);
    }

    public RedisClient(Properties prop, int db, int maxTotal) {
        initDo(prop, db, maxTotal);
    }

    public RedisClient(String server, String user, String password, int db, int maxTotal) {
        initDo(server, user, password, db, maxTotal, 0L);
    }

    public RedisClient(String server, String user, String password, int db, int maxTotal, long maxWaitMillis) {
        initDo(server, user, password, db, maxTotal, maxWaitMillis);
    }

    private void initDo(Properties prop, int db, int maxTotal) {
        String server = prop.getProperty("server");
        String user = prop.getProperty("user");
        String password = prop.getProperty("password");
        String maxWaitMillis = prop.getProperty("maxWaitMillis");
        String maxTotalStr = prop.getProperty("maxTotal");

        this.keyPrefix = prop.getProperty("keyPrefix");

        if (maxTotal > 0) {
            initDo(server,
                    user,
                    password,
                    db,
                    maxTotal,
                    (TextUtil.isEmpty(maxWaitMillis) ? 0L : Long.parseLong(maxWaitMillis))
            );
        } else {
            initDo(server,
                    user,
                    password,
                    db,
                    (TextUtil.isEmpty(maxTotalStr) ? 0 : Integer.parseInt(maxTotalStr)),
                    (TextUtil.isEmpty(maxWaitMillis) ? 0L : Long.parseLong(maxWaitMillis))
            );
        }
    }

    private void initDo(String server, String user, String password, int db, int maxTotal, long maxWaitMillis) {
        JedisPoolConfig config = new JedisPoolConfig();

        if (db < 0) {
            db = 0;
        }

        if (maxTotal < 20) {
            maxTotal = 200;
        }

        int maxIdle = maxTotal / 100;
        if (maxIdle < 5) {
            maxIdle = 5;
        }

        if (maxWaitMillis < 3000) {
            maxWaitMillis = 3000;
        }

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMaxWaitMillis(maxWaitMillis);
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);

        String[] ss = server.split(":");

        if ("".equals(password)) {
            password = null;
        }

        if (TextUtil.isEmpty(user)) {
            jedisPool = new JedisPool(config, ss[0], Integer.parseInt(ss[1]), 3000, password, db);
        } else {
            jedisPool = new JedisPool(config, ss[0], Integer.parseInt(ss[1]), 3000, user, password, db);
        }
    }

    //兼容旧的faas
    @Deprecated
    public void open0(Consumer<RedisSession> using) {
        open(using);
    }

    @Deprecated
    public <T> T open1(Function<RedisSession, T> using) {
        return openAndGet(using);
    }


    /**
     * 打开会话，不返回值
     */
    public void open(Consumer<RedisSession> using) {
        try (RedisSession session = openSession()) {
            using.accept(session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 打开会话，并要一个值
     */
    public <T> T openAndGet(Function<RedisSession, T> using) {
        try (RedisSession session = openSession()) {
            return using.apply(session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ////////////////////

    /**
     * 打开会话（需要自己关闭）
     */
    public RedisSession openSession() {
        Jedis jx = jedisPool.getResource();
        return new RedisSession(jx, keyPrefix);
    }

    ////////////////////

    /**
     * 获取一个原子数
     */
    public RedisAtomic getAtomic(String atomicName) {
        return new RedisAtomic(this, atomicName);
    }


    /**
     * 获取一个总线
     */
    public RedisBus getBus() {
        return new RedisBus(this);
    }

    /**
     * 获取一个存储桶
     */
    public RedisBucket getBucket() {
        return new RedisBucket(this);
    }

    /**
     * 获取一个哈希
     */
    public RedisHash getHash(String hashName) {
        return new RedisHash(this, hashName);
    }

    /**
     * 获取一个锁
     */
    public RedisLock getLock(String lockName) {
        return new RedisLock(this, lockName);
    }

    /**
     * 获取一个队列
     */
    public RedisQueue getQueue(String queueName) {
        return new RedisQueue(this, queueName);
    }

    /**
     * 获取一个Id生成器
     */
    public RedisId getId(String idName) {
        return new RedisId(this, idName);
    }

}