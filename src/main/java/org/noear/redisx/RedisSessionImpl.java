package org.noear.redisx;

import org.noear.redisx.model.LocalHash;
import org.noear.redisx.utils.AssertUtil;
import org.noear.redisx.utils.TextUtil;
import redis.clients.jedis.*;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.Tuple;

import java.util.*;

/**
 * Redis 会话
 *
 * @author noear
 * @since 1.0
 */
public class RedisSessionImpl implements RedisSession {

    private static final String LOCK_SUCCEED = "OK";

    private final UnifiedJedis jedis;

    protected RedisSessionImpl(UnifiedJedis jedis) {
        this.jedis = jedis;
    }

    /**
     * 关闭会话
     */
    @Override
    public void close() throws Exception {
        //不再需要关闭了
    }

    private String _key;
    private long _seconds;

    /**
     * 获取jedis原始对象
     */
    @Override
    public UnifiedJedis jedis() {
        return jedis;
    }

    /**
     * 删除一批主键
     */
    @Override
    public Long deleteKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        String[] keyAry = new String[keys.size()];
        return jedis.del(keys.toArray(keyAry));
    }

    /**
     * 检查一批主键是否存在
     */
    @Override
    public Long existsKeys(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        String[] keyAry = new String[keys.size()];
        return jedis.exists(keys.toArray(keyAry));
    }

    /**
     * 主键
     */
    @Override
    public RedisSessionImpl key(String key) {
        AssertUtil.notEmpty(key, "redis key cannot be empty");
        _key = key;
        return this;
    }

    /**
     * 设置超时（一般跟在 key 后面）
     *
     * @param seconds 秒数（+num 过期秒数；-1永不过期）
     */
    @Override
    public RedisSessionImpl expire(int seconds) {
        _seconds = seconds;
        return this;
    }

    /**
     * 设置为持续存在（即不超时）
     */
    @Override
    public RedisSessionImpl persist() {
        _seconds = -1;
        return this;
    }

    private void expirePush() {
        //+x: 具体时间
        if (_seconds > 0) {
            jedis.expire(_key, _seconds);
        }

        //-1: 永久
        if (_seconds == -1) {
            jedis.persist(_key); //持续存在
        }

        //-2:过期或已删
    }


    /**
     * 尝试延期
     */
    @Override
    public void delay() {
        expirePush();
    }

    /**
     * 尝试延期
     */
    @Override
    public void delay(int seconds) {
        _seconds = seconds;
        expirePush();
    }

    /**
     * 主键扫描
     *
     * @param keyPattern 模式（?表示1+, *表示0+）
     */
    @Override
    public List<String> scan(String keyPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(keyPattern);

        return jedis.scan(cursor, p).getResult();
    }

    /**
     * 主键匹配
     *
     * @param keyPattern 模式（?表示1+, *表示0+）
     */
    @Override
    public boolean match(String keyPattern) {
        List<String> temp = scan(keyPattern, 1);
        return (temp != null && temp.size() > 0);
    }


    /**
     * 主键是否存在
     */
    @Override
    public Boolean exists() {
        return jedis.exists(_key);
    }

    /**
     * 主键删除
     */
    @Override
    public Boolean delete() {
        return jedis.del(_key) > 0;
    }

    /**
     * 主键重命名
     */
    @Override
    public void rename(String newKey) {
        jedis.rename(_key, newKey);
    }


    /**
     * 获取剩余时间
     */
    @Override
    public long ttl() {
        return jedis.ttl(_key);
    }

    /**
     * 获取主键
     */
    @Override
    public Set<String> keys(String pattern) {
        return jedis.keys(pattern);
    }

    //------
    //value::

    /**
     * 设置主键对应的值
     */
    @Override
    public RedisSessionImpl set(String val) {
        AssertUtil.notNull(val, "redis value cannot be null");

        if (_seconds > 0) {
            SetParams options = new SetParams().ex(_seconds);
            jedis.set(_key, val, options);
        } else {
            jedis.set(_key, val);
        }

        return this;
    }

    /**
     * 设置主键对应的值
     */
    @Override
    public RedisSessionImpl set(long val) {
        return set(String.valueOf(val));
    }



    /**
     * 获取主键对应的值
     */
    @Override
    public String get() {
        return jedis.get(_key);
    }

    /**
     * 获取主键对应的值，并转为长整型
     */
    @Override
    public long getAsLong() {
        String temp = get();
        if (TextUtil.isEmpty(temp)) {
            return 0L;
        } else {
            return Long.parseLong(temp);
        }
    }


    /**
     * 获取多个主键值
     */
    @Override
    public List<String> getMore(String... keys) {
        return jedis.mget(keys);
    }


    /**
     * 主键对应的值，原子增量
     */
    @Override
    public long incr(long num) {
        long val = jedis.incrBy(_key, num);
        expirePush();

        return val;
    }

    /**
     * 主键对应的值，原子增量
     */
    @Override
    public long incr() {
        long val = jedis.incr(_key);
        expirePush();

        return val;
    }

    /**
     * 主键对应的值，原子减量
     */
    @Override
    public long decr() {
        long val = jedis.decr(_key);
        expirePush();

        return val;
    }

    /**
     * 主键尝试锁一个值
     */
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
        String rst = jedis.set(_key, val, options); //设置成功，返回 1 。//设置失败，返回 0 。

        return LOCK_SUCCEED.equals(rst);//成功获得锁
    }

    /**
     * 主键尝试锁
     */
    @Override
    public boolean lock() {
        return lock(System.currentTimeMillis() + "");
    }


    //--------
    //hash::

    @Override
    public Boolean hashHas(String field) {
        return jedis.hexists(_key, field);
    }

    /**
     * 哈希字段扫描
     *
     * @param fieldPattern 字段模式（?表示1+, *表示0+）
     */
    @Override
    public List<Map.Entry<String, String>> hashScan(String fieldPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(fieldPattern);

        return jedis.hscan(_key, cursor, p).getResult();
    }

    /**
     * 哈希字段匹配
     */
    @Override
    public boolean hashMatch(String fieldPattern) {
        List<Map.Entry<String, String>> temp = hashScan(fieldPattern, 1);

        return (temp != null && temp.size() > 0);
    }

    /**
     * 哈希字段删除
     */
    @Override
    public long hashDel(String... fields) {
        return jedis.hdel(_key, fields);
    }

    /**
     * 哈希字段设置
     */
    @Override
    public RedisSessionImpl hashSet(String field, String val) {
        jedis.hset(_key, field, val);
        expirePush();

        return this;
    }

    /**
     * 哈希字段设置
     */
    @Override
    public RedisSessionImpl hashSet(String field, long val) {
        return hashSet(field, String.valueOf(val));
    }

    /**
     * 哈希字段批量设置（管道模式操作）
     */
    @Override
    public RedisSessionImpl hashSetAll(Map<? extends String, ? extends String> map) {
        Map<String,String> map2 = new HashMap<>(map);
        jedis.hset(_key, map2);

        expirePush();

        return this;
    }


    /**
     * 哈希字段增量操作
     */
    @Override
    public long hashIncr(String field, long num) {
        long val = jedis.hincrBy(_key, field, num);
        expirePush();

        return val;
    }

    /**
     * 哈希字段获取
     */
    @Override
    public String hashGet(String field) {
        return jedis.hget(_key, field);
    }

    /**
     * 哈希字段获取并转为长整型
     */
    @Override
    public long hashGetAsLong(String field) {
        String temp = hashGet(field);

        if (TextUtil.isEmpty(temp)) {
            return 0;
        } else {
            return Long.parseLong(temp);
        }
    }

    /**
     * 哈希字段多个获取
     */
    @Override
    public List<String> hashGetMore(String... fields) {
        return jedis.hmget(_key, fields);
    }

    /**
     * 哈希获取所有字段
     */
    @Override
    public LocalHash hashGetAll() {
        return new LocalHash(jedis.hgetAll(_key));
    }

    /**
     * 哈希获取所有字段名
     */
    @Override
    public Set<String> hashGetAllKeys() {
        return jedis.hkeys(_key);
    }

    /**
     * 哈希获取所有字段值
     */
    @Override
    public List<String> hashGetAllValues() {
        return jedis.hvals(_key);
    }

    /**
     * 哈希长度
     */
    @Override
    public long hashLen() {
        return jedis.hlen(_key);
    }


    //------------------
    //list::

    /**
     * 列表添加项
     */
    @Override
    public RedisSessionImpl listAdd(String item) {
        jedis.lpush(_key, item); //左侧压进
        expirePush();

        return this;
    }

    /**
     * 列表添加项
     */
    @Override
    public RedisSessionImpl listAdd(long item) {
        return listAdd(String.valueOf(item));
    }

    /**
     * 列表设置位置对应的项
     */
    @Override
    public RedisSessionImpl listSet(int index, String newValue) {
        jedis.lset(_key, index, newValue);
        expirePush();

        return this;
    }

    /**
     * 列表删除项
     * <p>
     * count > 0 : 从表头开始向表尾搜索，移除与 VALUE 相等的元素，数量为 COUNT 。
     * count < 0 : 从表尾开始向表头搜索，移除与 VALUE 相等的元素，数量为 COUNT 的绝对值。
     * count = 0 : 移除表中所有与 VALUE 相等的值。
     */
    @Override
    public RedisSessionImpl listDel(String item, int count) {
        jedis.lrem(_key, count, item); //左侧压进
        expirePush();

        return this;
    }

    /**
     * 列表删除项
     */
    @Override
    public RedisSessionImpl listDel(String item) {
        return listDel(item, 0);
    }

    @Override
    public RedisSessionImpl listDelRange(Collection<? extends String> items) {
        for (String item : items) {
            jedis.lrem(_key, 0, item); //左侧压进
        }

        expirePush();

        return this;
    }

    /**
     * 列表批量添加项
     */

    @Override
    public RedisSessionImpl listAddRange(Collection<? extends String> items) {
        jedis.lpush(_key, items.toArray(new String[items.size()]));

        expirePush();

        return this;
    }

    /**
     * 列表冒出
     */
    @Override
    public String listPop() {
        return jedis.rpop(_key); //右侧推出
    }

    /**
     * 列表预览
     */
    @Override
    public String listPeek() {
        return jedis.lindex(_key, -1);  //右侧推出（先进先出）
    }

    /**
     * 列表获取项（先进先出，从right 取）
     */
    @Override
    public String listGet(int index) {
        return jedis.lindex(_key, index); //从right取
    }

    /**
     * 列表分页获取项（先进先出，从right取）
     */
    @Override
    public List<String> listGetRange(int start, int end) {
        return jedis.lrange(_key, start, end);
    }

    @Override
    public List<String> listGetAll() {
        return jedis.lrange(_key, 0, -1);
    }

    /**
     * 列表长度
     */
    @Override
    public long listLen() {
        return jedis.llen(_key);
    }

    //------------------
    //Sset::
    @Override
    public long setAdd(String item) {
        long tmp = jedis.sadd(_key, item); //左侧压进
        expirePush();

        return tmp;
    }

    @Override
    public long setDel(String item) {
        long tmp = jedis.srem(_key, item); //左侧压进
        return tmp;
    }

    @Override
    public RedisSessionImpl setAddRange(Collection<String> items) {
        jedis.sadd(_key, items.toArray(new String[items.size()])); //左侧压进

        expirePush();

        return this;
    }

    @Override
    public long setLen() {
        return jedis.scard(this._key);
    }

    @Override
    public String setPop() {
        return jedis.spop(_key); //右侧推出
    }

    @Override
    public List<String> setGet(int count) {
        return jedis.srandmember(_key, count);
    }

    @Override
    public List<String> setScan(String itemPattern, int count) {
        String cursor = ScanParams.SCAN_POINTER_START;

        ScanParams p = new ScanParams();
        p.count(count);
        p.match(itemPattern);

        return jedis.sscan(_key, cursor, p).getResult();
    }

    @Override
    public boolean setMatch(String itemPattern) {
        List<String> temp = setScan(itemPattern, 1);
        return (temp != null && temp.size() > 0);
    }

    //------------------
    //Sort set::
    @Override
    public RedisSessionImpl zsetAdd(double score, String item) {
        jedis.zadd(_key, score, item);
        expirePush();

        return this;
    }

    @Override
    public long zsetDel(String... items) {
        long tmp = jedis.zrem(_key, items);
        return tmp;
    }

    @Override
    public long zsetLen() {
        return jedis.zcard(_key);
    }

    @Override
    public Collection<String> zsetGet(long start, long end) {
        return jedis.zrange(_key, start, end);
    }


    @Override
    public long zsetIdx(String item) {
        Long tmp = jedis.zrank(_key, item);
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

        return jedis.zscan(_key, cursor, p).getResult();
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
        long tmp = jedis.geoadd(_key, lng, lat, member);
        expirePush();
        return tmp;
    }

    @Override
    public long geoAddAll(Map<String, GeoCoordinate> memberMap) {
        long tmp = jedis.geoadd(_key, memberMap);
        expirePush();
        return tmp;
    }

    @Override
    public List<GeoRadiusResponse> geoGetByRadius(double centerLng, double centerLat, long radius) {
        return jedis.georadius(_key, centerLng, centerLat, radius, GeoUnit.M);
    }

    @Override
    public long geoDist(String member1, String member2) {
        return jedis.geodist(_key, member1, member2, GeoUnit.M).longValue();
    }

    //------------------
    //message::
    @Override
    public long publish(String channel, String message) {
        return jedis.publish(channel, message);
    }

    @Override
    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
        jedis.subscribe(jedisPubSub, channels);
    }

}
