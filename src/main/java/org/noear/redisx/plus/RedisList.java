package org.noear.redisx.plus;

import org.noear.redisx.RedisClient;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author noear
 * @since 1.3
 */
public class RedisList implements Iterable<String>{
    private final RedisClient client;
    private final String listName;

    public RedisList(RedisClient client, String listName) {
        this.client = client;
        this.listName = listName;
    }

    public boolean remove(String element) {
        client.openAndGet(s -> s.key(listName).listDel(element));
        return true;
    }

    public String removeAt(int index) {
        return client.openAndGet(s -> {
            String element = s.key(listName).listGet(index);
            if (element != null) {
                s.listDel(element);
            }
            return element;
        });
    }

    public boolean removeAll(Collection<? extends String> c) {
        client.openAndGet(s -> s.key(listName).listDelRange(c));
        return true;
    }

    public String get(int index) {
        return client.openAndGet(s -> s.key(listName).listGet(index));
    }

    public boolean add(String element) {
        client.openAndGet(s -> s.key(listName).listAdd(element));
        return true;
    }

    public boolean addAll(Collection<? extends String> elements) {
        client.open(s -> s.key(listName).listAddRange(elements));
        return true;
    }

    public List<String> subList(int fromIndex, int toIndex) {
        return client.openAndGet(s -> s.key(listName).listGetRange(fromIndex, toIndex));
    }

    @Override
    public Iterator<String> iterator() {
        return client.openAndGet(s -> s.key(listName).listGetAll()).iterator();
    }

    public Object[] toArray() {
        return client.openAndGet(s -> s.key(listName).listGetAll()).toArray();
    }

    public long size() {
        return client.openAndGet(s -> s.key(listName).listLen());
    }

    public void clear() {
        client.open(s -> s.key(listName).delete());
    }
}
