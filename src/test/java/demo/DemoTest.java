package demo;

import demo.model.OrderDo;
import demo.model.UserDo;
import org.junit.jupiter.api.Test;
import org.noear.redisx.RedisSession;
import org.noear.redisx.plus.*;
import org.noear.solon.annotation.Inject;
import org.noear.redisx.RedisClient;
import org.noear.solon.test.SolonTest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author noear 2021/10/12 created
 */
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

    @Test
    public void test() {
        //写操作:: key().expire().xxx()
        RedisSession session = client.openSession();
        session.key("order:1").expire(10).set("hello");

        //读操作:: key().xxx();
        String item_1 = session.key("order:1").get();
        assert item_1 != null;

        //写操作:: key().expire().xxx()
        session.key("user:1").expire(10)
                .hashSet("name", "noear")
                .hashSet("sex", "1");

        //延时操作:: key().delay()
        session.key("user_link:1").delay(10);

        assert "1".equals(session.key("user:1").hashGet("sex"));

        assert true;

        session.key("user:2").hashInit("sex", "1");
        System.out.println(session.key("user:2").hashGet("sex"));
        assert "1".equals(session.key("user:2").hashGet("sex"));

        session.key("user:2").hashInit("sex", "2");
        System.out.println(session.key("user:2").hashGet("sex"));
        assert "1".equals(session.key("user:2").hashGet("sex"));
    }

    @Test
    public void test_bucket() throws Exception {
        //--- bucket 使用
        RedisBucket bucket = client.getBucket();

        //存储
        bucket.store("item:1", "hello", 2);

        //获取
        assert "hello".equals(bucket.get("item:1"));

        //延时
        bucket.delay("item:1", 1);

        Thread.sleep(4 * 1000);

        //4秒后已过时
        assert "hello".equals(bucket.get("item:1")) == false;

    }

    @Test
    public void test_bucket2() throws Exception {
        //--- bucket 带序列化的使用
        RedisBucket bucket = client.getBucket();

        UserDo userDo = new UserDo();
        userDo.id = 1212;
        userDo.name = "noear";
        userDo.create_lat = 12.111111;
        userDo.create_lng = 121239123.12;
        userDo.create_time = new Date();


        UserDo userDo2 = bucket.getOrStoreAndSerialize("userex:1212", 2, UserDo.class, () -> userDo);

        UserDo userDo3 = bucket.getAndDeserialize("userex:1212", UserDo.class);

        assert userDo2 != null;
        assert userDo3 != null;

        assert userDo2.id == userDo3.id;
        assert userDo2.create_time.getTime() == userDo3.create_time.getTime();


        //移除一批匹配模式的主键
        bucket.removeByPattern("item:");
    }

    @Test
    public void test_hash() {
        //--- hash 使用
        RedisHash redisHash = client.getHash("user:121");

        redisHash.put("id", 1);
        redisHash.put("name", "demo");

        OrderDo orderDo = new OrderDo();
        orderDo.id = 10001;
        orderDo.traceId = "demo";
        orderDo.note = "test demo";
        redisHash.putAndSerialize("order", orderDo);

        assert redisHash.getAsInt("id") == 1;

        OrderDo orderDo1 = redisHash.getAndDeserialize("order", OrderDo.class);
        assert orderDo1.id == orderDo.id;

        assert redisHash.size() == 3;
    }

    @Test
    public void test_hashAll() {
        //--- hash 使用
        RedisHash redisHash = client.getHash("user:112");

        Map<String, String> map = new HashMap<>();
        map.put("id", "1");
        map.put("name", "demo");

        redisHash.putAll(map);

        OrderDo orderDo = new OrderDo();
        orderDo.id = 10001;
        orderDo.traceId = "demo";
        orderDo.note = "test demo";
        redisHash.putAndSerialize("order", orderDo);

        assert redisHash.getAsInt("id") == 1;

        OrderDo orderDo1 = redisHash.getAndDeserialize("order", OrderDo.class);
        assert orderDo1.id == orderDo.id;

        assert redisHash.size() == 3;
    }

    @Test
    public void test_id() {
        //--- id 使用
        RedisId redisId = client.getId("id:user");

        long user_id = 10000 + redisId.generate();
        long order_id = 1000000 + redisId.generate();

        assert user_id > 10000;
        assert order_id > 1000000;
    }

    @Test
    public void test_lock() {
        //--- lock 使用
        if (client.getLock("user:121212").tryLock()) {
            assert true;
            //业务处理
        } else {
            assert false;
            //提示：请不要频繁提交
        }
    }

    @Test
    public void test_atomic() {
        //--- atomic 使用
        RedisAtomic atomic = client.getAtomic("user_count");

        long num = atomic.get();

        atomic.increment();
        atomic.incrementBy(2);

        assert atomic.get() == (num + 3);
    }

    @Test
    public void test_list() {
        //--- list 使用
        RedisList list = client.getList("list:test");
        list.clear();

        list.add("1");
        list.add("2");

        assert "1".equals(list.get(0));
        assert "2".equals(list.get(1));
        assert list.get(2) == null;

        list.add("3");
        list.add("4");

        assert "3".equals(list.get(2));
        assert "4".equals(list.get(3));

        list.removeAt(3);
        assert list.get(3) == null;

        for (String item : list.getAll()) {
            System.out.println("test_list: " + item);
        }
    }

    @Test
    public void test_queue() {
        //--- queue 使用
        RedisQueue queue = client.getQueue("queue:test");
        queue.clear();

        queue.add("1");
        queue.add("2");

        assert "1".equals(queue.pop());
        assert "2".equals(queue.pop());
        assert queue.pop() == null;

        queue.add("3");
        queue.add("4");

        assert "3".equals(queue.peek());
        assert "3".equals(queue.peek());

        queue.popAll(item -> {
            System.out.println("test_queue: " + item);
        });
    }

    @Test
    public void test_bus() throws Exception {
        int count = 10;
        CountDownLatch countDownLatch = new CountDownLatch(count);

        //--- bus 使用
        RedisBus bus = client.getBus();

        //发消息 （如果没有订阅者，好像消息会白发）

        //订阅消息（这个函数会卡住线程）
        bus.subscribeFuture((topic, message) -> {
            System.out.println(topic + " = " + message);
            countDownLatch.countDown();
        }, "topic:test");

        Thread.sleep(100);

        for (int i = 0; i < count; i++) {
            bus.publish("topic:test", "event-" + System.currentTimeMillis());
        }

        countDownLatch.await(2, TimeUnit.SECONDS);

        assert countDownLatch.getCount() == 0;
    }
}
