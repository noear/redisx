### 1.3.9
* 增加 RedisBucket::exists 接口
* 增加 RedisBucket::existsByKeys 接口
* 增加 RedisBucket::existsByPattern 接口
* 增加 RedisBucket::removeByKeys 接口
* 增加 RedisBucket::removeByPattern 接口
* 增加 RedisSession::jedis 接口
* 增加 RedisSession::deleteKeys 接口
* 增加 RedisSession::existsKeys 接口
* 取消 key 前缀支持
* 升级 jedis 到 3.7.1 

### 1.3.6
* 添加前key缀支持 (1.3.9 取消了；容易晕)