package cn.wildfirchat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 投票功能配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "poll")
public class PollConfig {

    /**
     * 通知配置
     */
    private Notification notification = new Notification();

    @Data
    public static class Notification {
        /**
         * 系统通知用户ID，用于发送投票结果通知
         */
        private String userId = "system";
    }
}
