package cn.wildfirchat.entity.poll;

import lombok.Data;

import javax.persistence.*;

/**
 * 投票实体
 */
@Data
@Entity
@Table(name = "poll")
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", length = 64)
    private String groupId;

    @Column(name = "creator_id", nullable = false, length = 64)
    private String creatorId;

    @Column(nullable = false)
    private String title;

    private String description;

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
    @Column(name = "max_select")
    private Integer maxSelect;

    /**
     * 是否匿名：0=实名，1=匿名
     */
    private Integer anonymous;

    /**
     * 结束时间戳
     */
    @Column(name = "end_time")
    private long endTime;

    /**
     * 状态：0=进行中，1=已结束
     */
    private Integer status;

    /**
     * 删除标记：0=未删除，1=已删除
     */
    private Integer deleted;

    /**
     * 是否始终显示结果：0=投票前隐藏，1=始终显示
     */
    @Column(name = "show_result")
    private Integer showResult;

    @Column(name = "created_at")
    private long createdAt;

    @Column(name = "updated_at")
    private long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = System.currentTimeMillis();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = 0;
        if (visibility == null) visibility = 1;
        if (type == null) type = 1;
        if (maxSelect == null) maxSelect = 1;
        if (anonymous == null) anonymous = 0;
        if (showResult == null) showResult = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = System.currentTimeMillis();
    }
}
