package com.xiaoqi.usercenter.config;


import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class RedissonConfig {

    private String host;
    private String port;

    @Bean
    public RedissonClient redisClient() {
        //创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s", host, port);
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(3);
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;

    }


}
