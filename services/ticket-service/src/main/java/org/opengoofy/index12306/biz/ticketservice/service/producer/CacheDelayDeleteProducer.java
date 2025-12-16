package org.opengoofy.index12306.biz.ticketservice.service.producer;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.opengoofy.index12306.biz.ticketservice.dao.entity.CacheDelayDeleteDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheDelayDeleteProducer {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String STREAM_KEY = "cache:delay:delete:stream";

    /**
     * @param message 消息队列里需要发送的消息
     */
    public void send(CacheDelayDeleteDO message) {
        Map<String, String> body = new HashMap<>();
        body.put("cacheKey", message.getCacheKey());
        body.put("delayTime", String.valueOf(message.getDelayTime()));
        body.put("sendTime", String.valueOf(message.getSendTime()));

        stringRedisTemplate.opsForStream()
                .add(STREAM_KEY, body);
    }
}
