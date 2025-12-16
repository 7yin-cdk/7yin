package org.opengoofy.index12306.biz.ticketservice.service.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheDelayDeleteConsumer {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Redis Stream 的 key
     * 用于存储延迟删除缓存的消息
     */
    private static final String STREAM_KEY = "cache:delay:delete:stream";

    /**
     * Redis Stream 消费者组名称
     */
    private static final String GROUP_NAME = "cache-delay-delete-group";

    /**
     * 消费者名称
     * 单实例可写死，多实例部署需保证唯一
     */
    private static final String CONSUMER_NAME = "cache-delay-delete-consumer-1";

    /**
     * 控制消费线程运行状态
     * 用于 Spring 容器优雅关闭
     */
    private volatile boolean running = true;

    /**
     * 单线程执行器
     * 用于后台消费 Redis Stream
     */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r ->
                    new Thread(r, "cache-delay-delete-thread")
            );

    /**
     * Spring Bean 初始化完成后自动执行
     * 启动 Redis Stream 消费线程
     */
    @PostConstruct
    public void start() {
        executor.execute(this::run);
    }

    /**
     * Spring 容器关闭前执行
     * 停止消费线程，释放资源
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        executor.shutdown();
    }

    /**
     * 消费主流程：
     * 1️⃣ 先处理 Pending 消息（防止宕机导致的消息丢失）
     * 2️⃣ 再持续消费新消息
     */
    private void run() {
        consumePending();
        consumeNewMessages();
    }

    /**
     * 处理 Pending List 中的消息
     *
     * Pending 消息指：
     * - 已被某个 consumer 拉取
     * - 但尚未 ACK
     * - 通常由于进程异常退出导致
     *
     * ReadOffset.from("0") 表示读取 Pending 消息
     */
    private void consumePending() {
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> records =
                        stringRedisTemplate.opsForStream().read(
                                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                                StreamReadOptions.empty().count(1),
                                StreamOffset.create(
                                        STREAM_KEY,
                                        ReadOffset.from("0")
                                )
                        );

                // Pending 已处理完成
                if (records == null || records.isEmpty()) {
                    break;
                }

                for (MapRecord<String, Object, Object> record : records) {
                    handleMessage(record);
                }

            } catch (Exception e) {
                log.error("处理 Pending 消息异常", e);
            }
        }
    }

    /**
     * 正常消费新消息
     *
     * ReadOffset.lastConsumed() 等价于 XREADGROUP ... >
     * 只会拉取从未被消费过的新消息
     */
    private void consumeNewMessages() {
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> records =
                        stringRedisTemplate.opsForStream().read(
                                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                                StreamReadOptions.empty()
                                        .count(1)
                                        .block(Duration.ofSeconds(2)),
                                StreamOffset.create(
                                        STREAM_KEY,
                                        ReadOffset.lastConsumed()
                                )
                        );

                if (records == null || records.isEmpty()) {
                    continue;
                }

                for (MapRecord<String, Object, Object> record : records) {
                    handleMessage(record);
                }

            } catch (Exception e) {
                log.error("消费新消息异常", e);
            }
        }
    }

    /**
     * 处理单条延迟删除缓存消息
     *
     * 消息示例：
     * {
     *   cacheKey  : "order:detail:123",
     *   delayTime : "500"
     * }
     */
    private void handleMessage(MapRecord<String, Object, Object> record)
            throws InterruptedException {

        Map<Object, Object> value = record.getValue();

        // 要删除的缓存 key
        String cacheKey = (String) value.get("cacheKey");

        // 延迟时间（毫秒）
        Long delayTime = Long.valueOf((String) value.get("delayTime"));

        // 延迟等待（延迟双删的第二次删除）
        Thread.sleep(delayTime);

        // 删除缓存（幂等操作，允许重复执行）
        stringRedisTemplate.delete(cacheKey);

        // ACK 确认消息已被成功消费
        stringRedisTemplate.opsForStream()
                .acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
    }
}
