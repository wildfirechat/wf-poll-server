package cn.wildfirchat.dto.poll;

import lombok.Data;

import java.util.List;

/**
 * 参与投票请求
 */
@Data
public class VoteRequest {
    private String groupId;
    /**
     * 选中的选项ID列表（单选传1个）
     */
    private List<Long> optionIds;
}
