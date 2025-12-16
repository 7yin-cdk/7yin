package org.opengoofy.index12306.biz.ticketservice.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CacheDelayDeleteDO {
    /**
     * 需要删除的缓存 key
     */
    private String cacheKey;

    /**
     * 延迟时间（毫秒）
     */
    private Long delayTime;

    /**
     * 发送时间（用于排查问题）
     */
    private Long sendTime;
}
