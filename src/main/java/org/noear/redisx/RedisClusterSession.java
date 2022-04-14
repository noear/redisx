package org.noear.redisx;

import org.noear.redisx.model.LocalHash;
import org.noear.redisx.utils.AssertUtil;
import org.noear.redisx.utils.TextUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.params.SetParams;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis 会话
 *
 * @author noear
 * @since 1.0
 */
public class RedisClusterSession implements RedisSession {
    private static final String LOCK_SUCCEED = "OK";

    private final JedisCluster cluster;

    protected RedisClusterSession(JedisCluster jedis) {
        this.cluster = jedis;
    }

    private boolean _close = false;

    @Override
    public void close() throws Exception {
        if (_close) {
            return;
        }

        cluster.close();
        _close = true;
    }

    private String _key;
    private long _seconds;

    @Override
    public Jedis jedis() {
        return null;
    }

    @Override
    public Long deleteKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        String[] keyAry = new String[keys.size()];
        return cluster.getClusterNodes().get(0).getResource().del(keys.toArray(keyAry));
    }

    @Override
    public Long existsKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        String[] keyAry = new String[keys.size()];
        return cluster.exists(keys.toArray(keyAry));
    }

    @Override
    public RedisClusterSession key(String key) {
        AssertUtil.notEmpty(key, "redis key cannot be empty");
        _key = key;
        return this;
    }

    @Override
    public RedisClusterSession expire(int seconds) {
        _seconds = seconds;
        return this;
    }

    @Override
    public RedisClusterSession persist() {
        _seconds = -1;
        return this;
    }

    private void expirePush() {
        if (_seconds > 0) {
            cluster.expire(_key, _seconds);
        }

        if (_seconds < 0) {
            cluster.persist(_key); //持续存在
        }
    }


    @Override
    public void delay() {
        expirePush();
    }

    @Override
    public void delay(int seconds) {
        _seconds = seconds;
        expirePush();
    }

    @Override
    public List<String> scan(String keyPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(keyPattern);

        return cluster.scan(cursor, p).getResult();
    }

    @Override
    public boolean match(String keyPattern) {
        List<String> temp = scan(keyPattern, 1);
        return (temp != null && temp.size() > 0);
    }


    @Override
    public Boolean exists() {
        return cluster.exists(_key);
    }

    @Override
    public Boolean delete() {
        return cluster.del(_key) > 0;
    }

    @Override
    public void rename(String newKey) {
        cluster.rename(_key, newKey);
    }


    @Override
    public long ttl() {
        return cluster.ttl(_key);
    }

    @Override
    public Set<String> keys(String pattern) {
        return cluster.keys(pattern);
    }

    //------
    //value::

    @Override
    public RedisClusterSession set(String val) {
        AssertUtil.notNull(val, "redis value cannot be null");

        cluster.set(_key, val);
        expirePush();

        return this;
    }

    @Override
    public RedisClusterSession set(long val) {
        return set(String.valueOf(val));
    }


    @Override
    public String get() {
        return cluster.get(_key);
    }

    @Override
    public long getAsLong() {
        String temp = get();
        if (TextUtil.isEmpty(temp)) {
            return 0L;
        } else {
            return Long.parseLong(temp);
        }
    }


    @Override
    public List<String> getMore(String... keys) {
        return cluster.mget(keys);
    }


    @Override
    public long incr(long num) {
        long val = cluster.incrBy(_key, num);
        expirePush();

        return val;
    }

    @Override
    public long incr() {
        long val = cluster.incr(_key);
        expirePush();

        return val;
    }

    @Override
    public long decr() {
        long val = cluster.decr(_key);
        expirePush();

        return val;
    }

    @Override
    public boolean lock(String val) {
        /**
         * NX: IF_NOT_EXIST（只在键不存在时，才对键进行设置操作）
         * XX: IF_EXIST（只在键已经存在时，才对键进行设置操作）
         *
         * EX: SET_WITH_EXPIRE_TIME for second
         * PX: SET_WITH_EXPIRE_TIME for millisecond
         * */

        SetParams options = new SetParams().nx().ex(_seconds);
        String rst = cluster.set(_key, val, options); //设置成功，返回 1 。//设置失败，返回 0 。

        return LOCK_SUCCEED.equals(rst);//成功获得锁
    }

    @Override
    public boolean lock() {
        return lock(System.currentTimeMillis() + "");
    }


    //--------
    //hash::

    @Override
    public Boolean hashHas(String field) {
        return cluster.hexists(_key, field);
    }

    @Override
    public List<Map.Entry<String, String>> hashScan(String fieldPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(fieldPattern);

        return cluster.hscan(_key.getBytes(StandardCharsets.UTF_8), cursor.getBytes(StandardCharsets.UTF_8), p)
                .getResult()
                .stream()
                .map(item -> new AbstractMap.SimpleEntry<>(new String(item.getKey()), new String(item.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public boolean hashMatch(String fieldPattern) {
        List<Map.Entry<String, String>> temp = hashScan(fieldPattern, 1);

        return (temp != null && temp.size() > 0);
    }

    @Override
    public long hashDel(String... fields) {
        return cluster.hdel(_key, fields);
    }

    @Override
    public RedisClusterSession hashSet(String field, String val) {
        cluster.hset(_key, field, val);
        expirePush();

        return this;
    }

    @Override
    public RedisClusterSession hashSet(String field, long val) {
        return hashSet(field, String.valueOf(val));
    }

    @Override
    public RedisClusterSession hashSetAll(Map<? extends String, ? extends String> map) {
        map.forEach((k, v) ->
                this.cluster.hset(_key, k, v)
        );

        expirePush();

        return this;
    }


    @Override
    public long hashIncr(String field, long num) {
        long val = cluster.hincrBy(_key, field, num);
        expirePush();

        return val;
    }

    @Override
    public String hashGet(String field) {
        return cluster.hget(_key, field);
    }

    @Override
    public long hashGetAsLong(String field) {
        String temp = hashGet(field);

        if (TextUtil.isEmpty(temp)) {
            return 0;
        } else {
            return Long.parseLong(temp);
        }
    }

    @Override
    public List<String> hashGetMore(String... fields) {
        return cluster.hmget(_key, fields);
    }

    @Override
    public LocalHash hashGetAll() {
        return new LocalHash(cluster.hgetAll(_key));
    }

    @Override
    public Set<String> hashGetAllKeys() {
        return cluster.hkeys(_key);
    }

    @Override
    public List<String> hashGetAllValues() {
        return cluster.hvals(_key);
    }

    @Override
    public long hashLen() {
        return cluster.hlen(_key);
    }


    //------------------
    //list::

    @Override
    public RedisClusterSession listAdd(String item) {
        cluster.lpush(_key, item); //左侧压进
        expirePush();

        return this;
    }

    @Override
    public RedisClusterSession listAdd(long item) {
        return listAdd(String.valueOf(item));
    }

    @Override
    public RedisClusterSession listSet(int index, String newValue) {
        cluster.lset(_key, index, newValue);
        expirePush();

        return this;
    }

    @Override
    public RedisClusterSession listDel(String item, int count) {
        cluster.lrem(_key, count, item); //左侧压进
        expirePush();

        return this;
    }

    @Override
    public RedisClusterSession listDel(String item) {
        return listDel(item, 0);
    }

    @Override
    public RedisClusterSession listDelRange(Collection<? extends String> items) {
        for (String item : items) {
            this.cluster.lrem(_key, 0, item); //左侧压进
        }

        expirePush();

        return this;
    }

    @Override
    public RedisClusterSession listAddRange(Collection<? extends String> items) {
        for (String item : items) {
            this.cluster.lpush(_key, item); //左侧压进
        }

        expirePush();

        return this;
    }

    @Override
    public String listPop() {
        return cluster.rpop(_key); //右侧推出
    }

    @Override
    public String listPeek() {
        return cluster.lindex(_key, -1);  //右侧推出（先进先出）
    }

    @Override
    public String listGet(int index) {
        return cluster.lindex(_key, index); //从right取
    }

    @Override
    public List<String> listGetRange(int start, int end) {
        return cluster.lrange(_key, start, end);
    }

    @Override
    public List<String> listGetAll() {
        return cluster.lrange(_key, 0, -1);
    }

    @Override
    public long listLen() {
        return cluster.llen(_key);
    }

    //------------------
    //Sset::
    @Override
    public long setAdd(String item) {
        long tmp = cluster.sadd(_key, item); //左侧压进
        expirePush();

        return tmp;
    }

    @Override
    public long setDel(String item) {
        long tmp = cluster.srem(_key, item); //左侧压进
        return tmp;
    }

    @Override
    public RedisClusterSession setAddRange(Collection<String> items) {
        this.cluster.sadd(_key, items.toArray(new String[items.size()])); //左侧压进

        expirePush();

        return this;
    }

    @Override
    public long setLen() {
        return cluster.scard(this._key);
    }

    @Override
    public String setPop() {
        return cluster.spop(_key); //右侧推出
    }

    @Override
    public List<String> setGet(int count) {
        return cluster.srandmember(_key, count);
    }

    @Override
    public List<String> setScan(String itemPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(itemPattern);

        return cluster.sscan(_key.getBytes(StandardCharsets.UTF_8), cursor.getBytes(StandardCharsets.UTF_8), p)
                .getResult()
                .stream()
                .map(String::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean setMatch(String itemPattern) {
        List<String> temp = setScan(itemPattern, 1);
        return (temp != null && temp.size() > 0);
    }

    //------------------
    //Sort set::
    @Override
    public RedisClusterSession zsetAdd(double score, String item) {
        cluster.zadd(_key, score, item);
        expirePush();

        return this;
    }

    @Override
    public long zsetDel(String... items) {
        long tmp = cluster.zrem(_key, items);
        return tmp;
    }

    @Override
    public long zsetLen() {
        return cluster.zcard(_key);
    }

    @Override
    public Set<String> zsetGet(long start, long end) {
        return cluster.zrange(_key, start, end);
    }


    @Override
    public long zsetIdx(String item) {
        Long tmp = cluster.zrank(_key, item);
        if (tmp == null) {
            return -1;
        } else {
            return tmp;
        }
    }

    @Override
    public List<Tuple> zsetScan(String itemPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(itemPattern);

        return cluster.zscan(_key.getBytes(StandardCharsets.UTF_8), cursor.getBytes(StandardCharsets.UTF_8), p).getResult();
    }

    @Override
    public boolean zsetMatch(String itemPattern) {
        List<Tuple> temp = zsetScan(itemPattern, 1);
        return (temp != null && temp.size() > 0);
    }

    //------------------
    //geo::

    @Override
    public long geoAdd(double lng, double lat, String member) {
        long tmp = cluster.geoadd(_key, lng, lat, member);
        expirePush();
        return tmp;
    }

    @Override
    public long geoAddAll(Map<String, GeoCoordinate> memberMap) {
        long tmp = cluster.geoadd(_key, memberMap);
        expirePush();
        return tmp;
    }

    @Override
    public List<GeoRadiusResponse> geoGetByRadius(double centerLng, double centerLat, long radius) {
        return cluster.georadius(_key, centerLng, centerLat, radius, GeoUnit.M);
    }

    @Override
    public long geoDist(String member1, String member2) {
        return cluster.geodist(_key, member1, member2, GeoUnit.M).longValue();
    }

    //------------------
    //message::


    @Override
    public long publish(String channel, String message) {
        return cluster.publish(channel, message);
    }

    @Override
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
        cluster.subscribe(jedisPubSub, channels);
    }

}
