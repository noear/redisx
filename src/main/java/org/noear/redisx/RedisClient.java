package org.noear.redisx;

import org.noear.redisx.plus.*;
import org.noear.redisx.utils.SerializerDefault;
import org.noear.redisx.utils.TextUtil;
import redis.clients.jedis.*;

import java.net.URI;
import java.time.Duration;
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
public class RedisClient implements AutoCloseable {
    /**
     * 统一接口
     */
    private UnifiedJedis unifiedJedis;
    private Serializer serializer = new SerializerDefault();

    public Serializer serializer() {
        return serializer;
    }

    public UnifiedJedis jedis(){
        return unifiedJedis;
    }

    public void serializer(Serializer serializer) {
        if (serializer != null) {
            this.serializer = serializer;
        }
    }

    public RedisClient(Properties prop) {
        String db = prop.getProperty("db");

        if (TextUtil.isEmpty(db)) {
            initDo(prop, 0, 0);
        } else {
            initDo(prop, Integer.parseInt(db), 0);
        }
    }

    public RedisClient(Properties prop, int db) {
        initDo(prop, db, 0);
    }

    public RedisClient(Properties prop, int db, int maxTotal) {
        initDo(prop, db, maxTotal);
    }

    private void initDo(Properties prop, int db, int maxTotal) {
        String serializerStr = prop.getProperty("serializer");
        if (TextUtil.isEmpty(serializerStr) == false) {
            prop.remove("serializer");
            try {
                Class<?> serializerClz = Class.forName(serializerStr);
                Serializer serializerNew = (Serializer) serializerClz.getDeclaredConstructor().newInstance();
                serializer(serializerNew);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        //1.转换参数
        String server = prop.getProperty("server");
        String user = prop.getProperty("user");
        String password = prop.getProperty("password");
        String maxWaitMillisStr = prop.getProperty("maxWaitMillis");
        String maxTotalStr = prop.getProperty("maxTotal");
        String maxIdleStr = prop.getProperty("maxIdle");
        String minIdleStr = prop.getProperty("minIdle");
        String connectionTimeoutStr = prop.getProperty("connectionTimeout");
        String socketTimeoutMillisStr = prop.getProperty("socketTimeoutMillis");

        String maxAttemptsStr = prop.getProperty("maxAttempts");

        long maxWaitMillis = (TextUtil.isEmpty(maxWaitMillisStr) ? 0L : Long.parseLong(maxWaitMillisStr));
        int maxAttempts = (TextUtil.isEmpty(maxAttemptsStr) ? 0 : Integer.parseInt(maxAttemptsStr));
        int maxIdle = (TextUtil.isEmpty(maxIdleStr) ? 0 : Integer.parseInt(maxIdleStr));
        int minIdle = (TextUtil.isEmpty(minIdleStr) ? 0 : Integer.parseInt(minIdleStr));
        int connectionTimeout = (TextUtil.isEmpty(connectionTimeoutStr) ? 0 : Integer.parseInt(connectionTimeoutStr));
        int socketTimeoutMillis = (TextUtil.isEmpty(socketTimeoutMillisStr) ? 0 : Integer.parseInt(socketTimeoutMillisStr));

        if (maxTotal == 0) {
            maxTotal = (TextUtil.isEmpty(maxTotalStr) ? 0 : Integer.parseInt(maxTotalStr));
        }


        if (db < 0) {
            db = 0;
        }

        if (maxTotal < 200) {
            maxTotal = 200;
        }

        if (maxWaitMillis < 3000) {
            maxWaitMillis = 3000;
        }

        if (maxAttempts == 0) {
            maxAttempts = 3;
        }

        if (connectionTimeout == 0) {
            connectionTimeout = 3000;
        }

        //2.构建连接池配置
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        if (maxTotal > 0) {
            poolConfig.setMaxTotal(maxTotal);
        }

        if (maxIdle > 0) {
            poolConfig.setMaxIdle(maxIdle);
        }

        if (minIdle > 0) {
            poolConfig.setMinIdle(minIdle);
        }

        if (maxWaitMillis > 0) {
            poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));
        }

        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestOnReturn(false);

        //3.构建客户端配置
        DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder();

        if (connectionTimeout > 0) {
            clientConfigBuilder.connectionTimeoutMillis(connectionTimeout);
        }

        if (socketTimeoutMillis > 0) {
            clientConfigBuilder.socketTimeoutMillis(socketTimeoutMillis);
        }

        if (TextUtil.isEmpty(password) == false) {
            clientConfigBuilder.password(password);
        }

        if (TextUtil.isEmpty(user) == false) {
            clientConfigBuilder.user(user);
        }

        clientConfigBuilder.database(db);

        DefaultJedisClientConfig clientConfig = clientConfigBuilder.build();


        //4.构建客户端
        if (server.contains(",")) {
            Set<HostAndPort> clusterNodes = new HashSet<>();
            for (String fqdn : server.split(",")) {
                if (TextUtil.isEmpty(fqdn) == false) {
                    clusterNodes.add(parseAddr(fqdn));
                }
            }

            this.unifiedJedis = new JedisCluster(clusterNodes, clientConfig, maxAttempts, poolConfig);
        } else {
            HostAndPort hostAndPort = parseAddr(server);
            this.unifiedJedis = new JedisPooled(poolConfig, hostAndPort, clientConfig);
        }
    }

    private HostAndPort parseAddr(String addr) {
        if (addr.contains("://")) {
            URI uri = URI.create(addr);
            return new HostAndPort(uri.getHost(), uri.getPort());
        } else {
            String[] hp = addr.split(":");
            return new HostAndPort(hp[0], Integer.parseInt(hp[1]));
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
        return new RedisSessionImpl(this.unifiedJedis);
    }

    ////////////////////

    /**
     * 获取一个原子数
     */
    public RedisAtomic getAtomic(String atomicName) {
        return new RedisAtomic(this, atomicName);
    }

    /**
     * 获取一个原子数
     */
    public RedisAtomic getAtomic(String atomicName, int inSeconds) {
        return new RedisAtomic(this, atomicName, inSeconds);
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

    @Override
    public void close() throws Exception {
        if (unifiedJedis != null) {
            unifiedJedis.close();
        }
    }
}
