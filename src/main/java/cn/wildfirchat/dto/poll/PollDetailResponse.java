package cn.wildfirchat.dto.poll;

import lombok.Data;

import java.util.List;

/**
 * 投票详情响应
 */
@Data
public class PollDetailResponse {
    private long id;
    private String groupId;
    private String creatorId;
    private String title;
    private String description;
    private Integer visibility;
    private Integer type;
    private Integer maxSelect;
    private Integer anonymous;
    private long endTime;
    private Integer status;
    private Integer showResult;
    private long createdAt;
    private long updatedAt;

    // 当前用户相关
    private Boolean hasVoted;
    private Boolean isCreator;
    private List<Long> myOptionIds; // 我选的选项

    // 统计
    private Integer totalVotes;     // 总票数（多选时可能大于人数）
    private Integer voterCount;     // 投票人数（去重后的用户数）

    // 选项列表
    private List<PollOptionVO> options;

    // 投票人详情（仅实名投票且是发起者时返回）
    private List<VoterDetailVO> voterDetails;

    @Data
    public static class PollOptionVO {
        private Long id;
        private String optionText;
        private Integer sortOrder;
        // 投票前未投票：不返回
        private Integer voteCount;
        private Integer votePercent;
    }

    @Data
    public static class VoterDetailVO {
        private Long optionId;
        private String optionText;
        private String userId;
        private String userName;
        private long createdAt;
    }
}
