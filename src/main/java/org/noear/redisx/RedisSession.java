package org.noear.redisx;

import org.noear.redisx.model.LocalHash;
import redis.clients.jedis.*;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisSession extends AutoCloseable {
    /**
     * 关闭会话
     */
    @Override
    void close() throws Exception;

    /**
     * 获取jedis原始对象
     */
    UnifiedJedis jedis();

    /**
     * 删除一批主键
     */
    Long deleteKeys(Collection<String> keys);

    /**
     * 检查一批主键是否存在
     */
    Long existsKeys(Collection<String> keys);

    /**
     * 主键
     */
    RedisSession key(String key);

    /**
     * 设置超时（一般跟在 key 后面）
     *
     * @param seconds 秒数（+num 过期秒数；-1永不过期）
     */
    RedisSession expire(int seconds);

    /**
     * 设置为持续存在（即不超时）
     */
    RedisSession persist();

    /**
     * 尝试延期
     */
    void delay();

    /**
     * 尝试延期
     */
    void delay(int seconds);

    /**
     * 主键扫描
     *
     * @param keyPattern 模式（?表示1+, *表示0+）
     */
    List<String> scan(String keyPattern, int count);

    /**
     * 主键匹配
     *
     * @param keyPattern 模式（?表示1+, *表示0+）
     */
    boolean match(String keyPattern);

    /**
     * 主键是否存在
     */
    Boolean exists();

    /**
     * 主键删除
     */
    Boolean delete();

    /**
     * 主键重命名
     */
    void rename(String newKey);

    /**
     * 获取剩余时间
     */
    long ttl();

    /**
     * 获取主键
     */
    Set<String> keys(String pattern);

    /**
     * 设置主键对应的值
     */
    RedisSession set(String val);

    /**
     * 设置主键对应的值
     */
    RedisSession set(long val);

    /**
     * 获取主键对应的值
     */
    String get();

    /**
     * 获取主键对应的值，并转为长整型
     */
    long getAsLong();

    /**
     * 获取多个主键值
     */
    List<String> getMore(String... keys);

    /**
     * 主键对应的值，原子增量
     */
    long incr(long num);

    /**
     * 主键对应的值，原子增量
     */
    long incr();

    /**
     * 主键对应的值，原子减量
     */
    long decr();

    /**
     * 主键尝试锁一个值
     */
    boolean lock(String val);

    /**
     * 主键尝试锁
     */
    boolean lock();

    Boolean hashHas(String field);

    /**
     * 哈希字段扫描
     *
     * @param fieldPattern 字段模式（?表示1+, *表示0+）
     */
    List<Map.Entry<String, String>> hashScan(String fieldPattern, int count);

    /**
     * 哈希字段匹配
     */
    boolean hashMatch(String fieldPattern);

    /**
     * 哈希字段删除
     */
    long hashDel(String... fields);

    /**
     * 哈希字段设置
     */
    RedisSession hashSet(String field, String val);

    /**
     * 哈希字段设置
     */
    RedisSession hashSet(String field, long val);

    /**
     * 哈希字段批量设置
     */
    RedisSession hashSetAll(Map<String, String> map);

    /**
     * 哈希字段增量操作
     */
    long hashIncr(String field, long num);

    /**
     * 哈希字段获取
     */
    String hashGet(String field);

    /**
     * 哈希字段获取并转为长整型
     */
    long hashGetAsLong(String field);

    /**
     * 哈希字段多个获取
     */
    List<String> hashGetMore(String... fields);

    /**
     * 哈希获取所有字段
     */
    LocalHash hashGetAll();

    /**
     * 哈希获取所有字段名
     */
    Set<String> hashGetAllKeys();

    /**
     * 哈希获取所有字段值
     */
    List<String> hashGetAllValues();

    /**
     * 哈希长度
     */
    long hashLen();

    /**
     * 列表添加项
     */
    RedisSession listAdd(String item);

    /**
     * 列表添加项
     */
    RedisSession listAdd(long item);

    /**
     * 列表设置位置对应的项
     */
    RedisSession listSet(int index, String newValue);

    /**
     * 列表删除项
     * <p>
     * count > 0 : 从表头开始向表尾搜索，移除与 VALUE 相等的元素，数量为 COUNT 。
     * count < 0 : 从表尾开始向表头搜索，移除与 VALUE 相等的元素，数量为 COUNT 的绝对值。
     * count = 0 : 移除表中所有与 VALUE 相等的值。
     */
    RedisSession listDel(String item, int count);

    /**
     * 列表删除项
     */
    RedisSession listDel(String item);

    RedisSession listDelRange(Collection<? extends String> items);

    /**
     * 列表批量添加项
     */
    RedisSession listAddRange(Collection<? extends String> items);

    /**
     * 列表冒出
     */
    String listPop();

    /**
     * 列表预览
     */
    String listPeek();

    /**
     * 列表获取项（先进先出，从right 取）
     */
    String listGet(int index);

    /**
     * 列表分页获取项（先进先出，从right取）
     */
    List<String> listGetRange(int start, int end);

    List<String> listGetAll();

    /**
     * 列表长度
     */
    long listLen();

    //------------------
    //Sset::
    long setAdd(String item);

    long setDel(String item);

    RedisSession setAddRange(Collection<String> items);

    long setLen();

    String setPop();

    List<String> setGet(int count);

    List<String> setScan(String itemPattern, int count);

    boolean setMatch(String itemPattern);

    //------------------
    //Sort set::
    RedisSession zsetAdd(double score, String item);

    long zsetDel(String... items);

    long zsetLen();

    Collection<String> zsetGet(long start, long end);

    long zsetIdx(String item);

    List<Tuple> zsetScan(String itemPattern, int count);

    boolean zsetMatch(String itemPattern);

    long geoAdd(double lng, double lat, String member);

    long geoAddAll(Map<String, GeoCoordinate> memberMap);

    List<GeoRadiusResponse> geoGetByRadius(double centerLng, double centerLat, long radius);

    long geoDist(String member1, String member2);

    long publish(String channel, String message);

    void subscribe(JedisPubSub jedisPubSub, String... channels);

}
