package cn.wildfirchat.entity.poll;

import lombok.Data;

import javax.persistence.*;

/**
 * 投票选项实体
 */
@Data
@Entity
@Table(name = "poll_option")
public class PollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 票数统计（冗余字段，实时更新）
     */
    @Column(name = "vote_count")
    private Integer voteCount;

    @Column(name = "created_at")
    private long createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
        if (voteCount == null) voteCount = 0;
        if (sortOrder == null) sortOrder = 0;
    }
}
