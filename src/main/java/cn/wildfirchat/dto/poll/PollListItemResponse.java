package cn.wildfirchat.dto.poll;

import lombok.Data;

/**
 * 投票列表项响应
 */
@Data
public class PollListItemResponse {
    private long id;
    private String groupId;
    private String creatorId;
    private String title;
    private Integer type;       // 1=单选, 2=多选
    private Integer anonymous;  // 0=实名, 1=匿名
    private Integer status;     // 0=进行中, 1=已结束
    private long endTime;
    private long createdAt;
    
    // 统计
    private Integer totalVotes;     // 总票数
    private Integer voterCount;     // 投票人数（去重后的用户数）
    
    // 当前用户相关
    private Boolean creator;    // 是否创建者（兼容旧字段）
    private Boolean isCreator;  // 是否创建者
    private Boolean hasVoted;   // 是否已投票
    private Boolean expired;    // 是否已过期
    
    // 状态标记
    private Boolean deleted;    // 是否已删除
}
