package cn.wildfirchat.dto.poll;

import lombok.Data;

import java.util.List;

/**
 * 创建投票请求
 */
@Data
public class CreatePollRequest {
    private String groupId;
    private String title;
    private String description;
    private List<String> options;
    /**
     * 可见性：1=仅群内，2=公开
     */
    private Integer visibility;
    /**
     * 类型：1=单选，2=多选
     */
    private Integer type;
    /**
     * 多选时最多选几项
     */
    private Integer maxSelect;
    /**
     * 是否匿名：0=实名，1=匿名
     */
    private Integer anonymous;
    private long endTime;
    /**
     * 是否始终显示结果：0=投票前隐藏，1=始终显示
     */
    private Integer showResult;
}
