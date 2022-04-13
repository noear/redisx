package demo;

import demo.model.UserDo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.redisx.plus.*;
import org.noear.solon.annotation.Inject;
import org.noear.redisx.RedisClient;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.SolonTest;

import java.util.Date;

/**
 * @author noear 2021/10/12 created
 */
@RunWith(SolonJUnit4ClassRunner.class)
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

    @Test
    public void test() {
        //写操作:: key().expire().xxx()
        client.open(session -> {
            session.key("order:1").expire(10).set("hello");
        });

        //读操作:: key().xxx();
        String item_1 = client.openAndGet(session -> session.key("order:1").get());
        assert item_1 != null;

        //写操作:: key().expire().xxx()
        client.open(session -> {
            session.key("user:1").expire(10)
                    .hashSet("name", "noear")
                    .hashSet("sex", "1");
        });

        //延时操作:: key().delay()
        client.open(session -> {
            session.key("user_link:1").delay(10);
        });

        assert true;
    }

    @Test
    public void test_bucket() throws Exception {
        //--- bucket 使用
        RedisBucket bucket = client.getBucket();

        bucket.store("item:1", "hello", 2);

        assert "hello".equals(bucket.get("item:1"));

        Thread.sleep(3 * 1000);

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


        UserDo userDo2 = bucket.getOrStoreAndSerialize("userex:1212", 2, () -> userDo);

        UserDo userDo3 =bucket.getAndDeserialize("userex:1212");

        assert userDo2 != null;
        assert userDo3 != null;

        assert userDo2.id == userDo3.id;
        assert userDo2.create_time.getTime() == userDo3.create_time.getTime();


        //移除一批匹配模式的主键
        bucket.removeByPattern("item:");
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

//    @Test
    public void test_bus() {
        //--- bus 使用
        RedisBus bus = client.getBus();

        //发消息 （如果没有订阅者，好像消息会白发）
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    bus.publish("topic:test", "event-" + System.currentTimeMillis());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //订阅消息（这个函数会卡住线程）
        bus.subscribe((topic, message) -> {
            System.out.println(topic + " = " + message);
        }, "topic:test");
    }
}
