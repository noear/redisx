package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

import java.util.Collection;
import java.util.List;

/**
 * Redis 列表
 *
 * @author noear
 * @since 1.3
 */
public class RedisList {
    private final RedisClient client;
    private final String listName;

    public RedisList(RedisClient client, String listName) {
        this.client = client;
        this.listName = listName;
    }

    /**
     * 移除项
     */
    public boolean remove(String element) {
        client.openAndGet(s -> s.key(listName).listDel(element));
        return true;
    }

    /**
     * 移除某个位置的项
     */
    public String removeAt(int index) {
        return client.openAndGet(s -> {
            String element = s.key(listName).listGet(index);
            if (element != null) {
                s.listDel(element);
            }
            return element;
        });
    }

    /**
     * 移除一个集合里的项
     */
    public boolean removeAll(Collection<? extends String> c) {
        client.openAndGet(s -> s.key(listName).listDelRange(c));
        return true;
    }

    /**
     * 获取某个位置的项
     */
    public String get(int index) {
        return client.openAndGet(s -> s.key(listName).listGet(index));
    }

    /**
     * 获取某个位置区间的项
     */
    public List<String> getRange(int fromIndex, int toIndex) {
        return client.openAndGet(s -> s.key(listName).listGetRange(fromIndex, toIndex));
    }

    /**
     * 获取所有项
     */
    public List<String> getAll() {
        return client.openAndGet(s -> s.key(listName).listGetAll());
    }

    /**
     * 添加项
     */
    public boolean add(String element) {
        client.openAndGet(s -> s.key(listName).listAdd(element));
        return true;
    }

    /**
     * 添加一个集合里的项
     */
    public boolean addAll(Collection<? extends String> elements) {
        client.open(s -> s.key(listName).listAddRange(elements));
        return true;
    }

    /**
     * 大小
     */
    public long size() {
        return client.openAndGet(s -> s.key(listName).listLen());
    }


    /**
     * 清空
     **/
    public void clear() {
        client.open(s -> s.key(listName).delete());
    }
}
