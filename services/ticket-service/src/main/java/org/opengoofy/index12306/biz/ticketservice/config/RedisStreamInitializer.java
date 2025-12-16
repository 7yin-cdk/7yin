package org.opengoofy.index12306.biz.ticketservice.config;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamInitializer implements InitializingBean {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STREAM_KEY = "cache:delay:delete:stream";
    private static final String GROUP_NAME = "cache-delay-delete-group";

    @Override
    public void afterPropertiesSet() {
        try {
            // 判断 stream 是否存在
            Boolean exists = stringRedisTemplate.hasKey(STREAM_KEY);
            if (Boolean.FALSE.equals(exists)) {
                // 创建 stream
                stringRedisTemplate.opsForStream().add(STREAM_KEY, Map.of("init", "init"));
            }

            // 创建消费者组（从头开始消费）
            stringRedisTemplate.opsForStream()
                    .createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
        } catch (Exception e) {
            // 如果消费者组已存在，会抛异常，忽略即可
        }
    }
}
