
[![Maven Central](https://img.shields.io/maven-central/v/org.noear/redisx.svg)](https://mvnrepository.com/search?q=g:org.noear%20AND%20redisx)
[![Apache 2.0](https://img.shields.io/:license-Apache2-blue.svg)](https://license.coscl.org.cn/Apache2/)
[![JDK-8+](https://img.shields.io/badge/JDK-8+-green.svg)](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
[![QQ交流群](https://img.shields.io/badge/QQ交流群-22200020-orange)](https://jq.qq.com/?_wv=1027&k=kjB5JNiC)

# RedisX

一个轻量级的 redis client ，基于 jedis 进行的友好封装。（就这样描述先...）


## 快速入门

### 1.配置

```xml
<dependency>
    <groupId>org.noear</groupId>
    <artifactId>redisx</artifactId>
    <version>1.6.11</version>
</dependency>
```

```yaml
test.rd1:
  server: localhost:6379
  db: 1
  password: 123456
```


可配置属性参考:


| 属性                  | 示例                   | 说明                                       |
|---------------------|----------------------|------------------------------------------|
| server              | "localhost:6379"     | 地址，"localhost:6379,localhost:6380" -集群模式 |
| db                  | 1                    | 库号，集群模式无效                                |
| password            | "123456"             | 密码                                       |
| user                | "user"               | 账号，redis 6.x 新加                          |
| maxTotal            | 200                  | 最大连接数                                    |
| maxIdle             | 200                  | 最大空闲数                                    |
| maxWaitMillis       | 3000                 | 最大建立连接等待时间                               |
| maxAttempts         | 5                    | 最大重试，集群模式有效                              |
| connectionTimeout   | 3000                 | 连接超时                                     |
| socketTimeoutMillis | 2000                 | Socket超时                                 |
| serializer          | "xxx.SerializerImpl" | 指定自定义序列化实现（一般不指定，用默认的）       |



### 2.bean 构建演示

* 用 solon 构建实例

```java
@Configuration
public class Config {
    @Bean
    public RedisClient redisClient(@Inject("${test.rd1}") RedisClient client) {
        return client;
    }
}
```

* 手动构建实例

```java
public class App {
    public static void main(String[] args){
        Properties props = new Properties();
        props.put("server", "localhost:6379");
        props.put("password","123456");
        
        RedisClient client = new RedisClient(props);
    }
}
```


### 3.基础会话接口操作

```java
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
        String tmp = client.openAndGet(session -> session.key("order:1").get());
        assert tmp != null;

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
}
```

## 领域增强对象接口

领域增强对象，是由基础会话接口封装而成

### RedisBucket - 存储桶

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

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


        UserDo userDo2 = bucket.getOrStoreAndSerialize("userex:1212", 2, () -> userDo);

        UserDo userDo3 =bucket.getAndDeserialize("userex:1212", UserDo.class);

        assert userDo2 != null;
        assert userDo3 != null;

        assert userDo2.id == userDo3.id;
        assert userDo2.create_time.getTime() == userDo3.create_time.getTime();


        //移除一批匹配模式的主键
        bucket.removeByPattern("item:");
    }
}

//实体要实现 Serializable 接口
public class UserDo implements Serializable {
    public long id;
    public String name;
    public double create_lng;
    public double create_lat;
    public Date create_time;
}
```


### RedisHash - 哈希表(兼容标准Map接口)

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

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
}

//实体要实现 Serializable 接口
public class OrderDo implements Serializable {
    public int id;
    public String traceId;
    public String note;
}
```


### RedisId - Id生成器

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

    @Test
    public void test_id() {
        //--- id 使用
        RedisId redisId = client.getId("id:user");

        long user_id = 10000 + redisId.generate();
        long order_id = 1000000 + redisId.generate();

        assert user_id > 10000;
        assert order_id > 1000000;
    }
}
```


### RedisLock - 分布式锁

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

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
}
```


### RedisAtomic - 原子数字

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

    @Test
    public void test_atomic() {
        //--- atomic 使用
        RedisAtomic atomic = client.getAtomic("user_count");

        long num = atomic.get();

        atomic.increment();
        atomic.incrementBy(2);

        assert atomic.get() == (num + 3);
    }
}
```

### RedisList - 列表

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

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
}
```



### RedisQueue - 队列

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

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
}
```


### RedisBus - 总线

```java
@SolonTest(DemoApp.class)
public class DemoTest {
    @Inject
    RedisClient client;

    @Test
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

        //异步订阅消息
        bus.subscribeFuture((topic, message) -> {
            System.out.println(topic + " = " + message);
        }, "topic:test");
    }
}
```