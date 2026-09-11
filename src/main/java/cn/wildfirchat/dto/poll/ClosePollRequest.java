package cn.wildfirchat.dto.poll;

import lombok.Data;

/**
 * 结束投票请求
 */
@Data
public class ClosePollRequest {
    private String groupId;
}
