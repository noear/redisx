### 1.6.6
* redis.clients 升为 5.1.5
* snack3 升为 3.2.111

### 1.6.5
* 优化 序列化对泛型的支持

### 1.6.4
* 添加 redis://localhost:6379 地址支持

### 1.6.3
* 添加 subscribeInThread
* 添加 新的时间结合 long time, TimeUnit timeUnit
* 添加 subscribeFuture

### 1.6.2
* snack3 升为 3.2.82
* jedis 升为 5.0.2
* 调整 SerializerJson 改回 带类型方案
* 添加 SerializerJsonNoType 不带类型 （补允 SerializerJson）

### 1.6.1
* SerializerJson 默认改成 stringify 方案 

### 1.6.0
* 移除一批弃用函数

### 1.5.1
* 添加一批新函数

### 1.5.0
* 调整 Serializer 接口（增加类型化 decode）

### 1.4.10
* jedis 升为 5.0.0
* snack3 升级为 3.2.80

### 1.4.9
* jedis 升为 4.4.3

### 1.4.8
* 增加对象里不直接调用 persist()
* session:set 改为 setex 指令（时间与值可同时提交）
* snack3 升级为 3.2.65

### 1.4.6
* snack3 升级为 3.2.62

### 1.4.5
* jedis 升级为 4.2.3
* 添加序列化json实现方案（非默认）
* 添加序列化配置支持

```yaml
test.rd1:
  server: "localhost:6379"
  password: 123456
  db: 1
  serializer: "org.noear.redisx.utils.SerializerJson"
```

### 1.4.2
* jedis 升级为 4.2.2
* 重新基于 UnifiedJedis 接口适配

### 1.4.1
* 增加集群模式支持

```yaml
test.rd1:
  server: "localhost:6379,localhost:6378" #带,号是集群模式标识
  password: 123456
```

### 1.3.11
* 增加 RedisClient::getHash(String hashName, int inSeconds) 接口
* 增加 RedisHahs::delay(int seconds) 接口

### 1.3.10
* 增加 RedisList 增强模型

### 1.3.9
* 取消 key 前缀支持

### 1.3.8
* 增加 RedisBucket::exists 接口
* 增加 RedisBucket::existsByKeys 接口
* 增加 RedisBucket::existsByPattern 接口
* 增加 RedisBucket::removeByKeys 接口
* 增加 RedisBucket::removeByPattern 接口
* 增加 RedisSession::jedis 接口
* 增加 RedisSession::deleteKeys 接口
* 增加 RedisSession::existsKeys 接口

### 1.3.6
* 添加前key缀支持 (1.3.9 取消了；容易晕)