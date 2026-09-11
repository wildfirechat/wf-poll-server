package cn.wildfirchat.entity.poll;

import lombok.Data;

import javax.persistence.*;

/**
 * 用户投票记录实体
 */
@Data
@Entity
@Table(name = "poll_vote", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"poll_id", "user_id", "option_id"}, name = "uk_poll_user_option")
})
public class PollVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "created_at")
    private long createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = System.currentTimeMillis();
    }
}
