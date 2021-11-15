
[![Maven Central](https://img.shields.io/maven-central/v/org.noear/redisx.svg)](https://mvnrepository.com/search?q=g:org.noear%20AND%20redisx)

` QQ交流群：22200020 `

# redisx

一个轻量级的 redis client ，基于 jedis 进行的友好封装。（就这样描述先...）


### 快速入门

#### 1.配置

```xml
<dependency>
    <groupId>org.noear</groupId>
    <artifactId>redisx</artifactId>
    <version>1.3</version>
</dependency>
```

```yaml
test.rd1:
  server: localhost:6379
  password: 123456
  db: 1
```

#### 2.bean 构建（以下代码以solon演示）
```java
@Configuration
public class Config {
    @Bean
    public RedisClient redisClient(@Inject("${test.rd1}") RedisClient client) {
        return client;
    }
}
```

#### 3.使用

```java
@RunWith(SolonJUnit4ClassRunner.class)
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

    @Test
    public void test() {
        //::redisX 基础接口使用

        client.open(session -> {
            session.key("order:1").expire(10).set("hello");
        });

        String item_1 = client.openAndGet(session -> session.key("order:1").get());

        assert item_1 != null;

        client.open(session -> {
            session.key("user:1").expire(10)
                    .hashSet("name", "noear")
                    .hashSet("sex", "1");
        });

        assert true;
    }

    @Test
    public void test_cache() throws Exception {
        //::redisX 增强接口使用

        //--- cache 使用
        RedisCache cache = client.getCache();
        cache.store("item:1", "hello", 2);

        assert "hello".equals(cache.get("item:1"));

        Thread.sleep(3 * 1000);

        assert "hello".equals(cache.get("item:1")) == false;


        //--- cache 带序列化的使用
        UserDo userDo = new UserDo();
        userDo.id = 1212;
        userDo.name = "noear";
        userDo.create_lat = 12.111111;
        userDo.create_lng = 121239123.12;
        userDo.create_time = new Date();

        //存储并序列化
        cache.storeAndSerialize("user:1212", userDo, 2);
        //获取并反序列化
        UserDo userDo1 = cache.getAndDeserialize("user:1212");
        assert userDo1 != null;

        System.out.println(userDo1);

        assert userDo1.create_lng == userDo.create_lng;
        assert userDo1.create_lat == userDo.create_lat;
        assert userDo1.create_time.getTime() == userDo.create_time.getTime();
    }

    @Test
    public void test_id() {
        //::redisX 增强接口使用

        //--- id 使用
        long user_id = 10000 + client.getId("id:user").generate();
        long order_id = 1000000 + client.getId("id:order").generate();

        assert user_id > 10000;
        assert order_id > 1000000;
    }

    @Test
    public void test_lock() {
        //::redisX 增强接口使用

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
        //::redisX 增强接口使用

        //--- atomic 使用
        RedisAtomic atomic = client.getAtomic("user_count");

        long num = atomic.get();

        atomic.increment();
        atomic.incrementBy(2);

        assert atomic.get() == (num + 3);
    }

    @Test
    public void test_queue() {
        //::redisX 增强接口使用

        //--- queue 使用
        RedisQueue queue = client.getQueue("queue:test");
        queue.add("1");
        queue.add("2");

        assert "1".equals(queue.pop());
        assert "2".equals(queue.pop());
        assert queue.pop() == null;

        queue.add("3");
        queue.add("4");

        queue.popAll(item -> {
            System.out.println("test5_queue: " + item);
        });
    }

    @Test
    public void test_bus() {
        //::redisX 增强接口使用

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

```