package org.noear.redisx;

import org.noear.redisx.plus.*;
import org.noear.redisx.utils.TextUtil;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Redis 客户端
 *
 * @author noear
 * @since 1.0
 */
public class RedisClient {

    /**
     * 连接池
     */
    private JedisPool jedisPool;
    /**
     * 集群连接池
     */
    private JedisCluster jedisCluster;

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
        String maxWaitMillisStr = prop.getProperty("maxWaitMillis");
        String maxTotalStr = prop.getProperty("maxTotal");

        long maxWaitMillis = (TextUtil.isEmpty(maxWaitMillisStr) ? 0L : Long.parseLong(maxWaitMillisStr));

        if (maxTotal == 0) {
            maxTotal = (TextUtil.isEmpty(maxTotalStr) ? 0 : Integer.parseInt(maxTotalStr));
        }

        initDo(server, user, password, db, maxTotal, maxWaitMillis);
    }

    private void initDo(String server, String user, String password, int db, int maxTotal, long maxWaitMillis) {
        try {
            initDo0(server, user, password, db, maxTotal, maxWaitMillis);
        } catch (RuntimeException e) {
            //调试时方便断点
            throw e;
        } catch (Throwable e) {
            //调试时方便断点
            throw new IllegalArgumentException(e);
        }
    }

    private void initDo0(String server, String user, String password, int db, int maxTotal, long maxWaitMillis) {
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


        final int client_timeout = 3000;
        final int client_maxAttempts = 64;
        final String server_separator = ",";

        // 判断是否为 Redis 集群
        if (server.contains(server_separator)) {
            Set<HostAndPort> nodes = new HashSet<>();
            for (String fqdn : server.split(server_separator)) {
                if (TextUtil.isEmpty(fqdn) == false) {
                    String[] info = fqdn.split(":");
                    nodes.add(new HostAndPort(info[0], Integer.parseInt(info[1])));
                }
            }

            if (TextUtil.isEmpty(user)) {
                this.jedisCluster = new JedisCluster(nodes, client_timeout, client_timeout, client_maxAttempts, password, config);
            } else {
                this.jedisCluster = new JedisCluster(nodes, client_timeout, client_timeout, client_maxAttempts, user, password, null, config);
            }
        } else {
            String[] ss = server.split(":");

            if ("".equals(password)) {
                password = null;
            }

            if (TextUtil.isEmpty(user)) {
                jedisPool = new JedisPool(config, ss[0], Integer.parseInt(ss[1]), client_timeout, password, db);
            } else {
                jedisPool = new JedisPool(config, ss[0], Integer.parseInt(ss[1]), client_timeout, user, password, db);
            }
        }
    }

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
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 打开会话，并要一个值
     */
    public <T> T openAndGet(Function<RedisSession, T> using) {
        try (RedisSession session = openSession()) {
            return using.apply(session);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    ////////////////////

    /**
     * 打开会话（需要自己关闭）
     */
    public RedisSession openSession() {
        if(this.jedisPool != null) {
            Jedis jx = jedisPool.getResource();
            return new RedisSingleSession(jx);
        } else {
            return new RedisClusterSession(this.jedisCluster);
        }
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
     * 获取一个哈希（永久存在）
     */
    public RedisHash getHash(String hashName) {
        return new RedisHash(this, hashName);
    }

    /**
     * 获取一个哈希（时效性）
     */
    public RedisHash getHash(String hashName, int inSeconds) {
        return new RedisHash(this, hashName, inSeconds);
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
     * 获取一个列表
     */
    public RedisList getList(String listName) {
        return new RedisList(this, listName);
    }

    /**
     * 获取一个Id生成器
     */
    public RedisId getId(String idName) {
        return new RedisId(this, idName);
    }

}
