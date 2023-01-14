### 1.4.5
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