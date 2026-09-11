package cn.wildfirchat.repository.poll;

import cn.wildfirchat.entity.poll.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    /**
     * 检查用户是否已在该投票中投票（单选/多选都只能用一次）
     */
    boolean existsByPollIdAndUserId(Long pollId, String userId);
    
    /**
     * 查询用户在该投票中的所有投票记录
     */
    List<PollVote> findByPollIdAndUserId(Long pollId, String userId);

    List<PollVote> findByPollId(Long pollId);

    long countByPollId(Long pollId);

    long countByPollIdAndOptionId(Long pollId, Long optionId);

    /**
     * 统计投票人数（按用户去重）
     */
    @Query("SELECT COUNT(DISTINCT v.userId) FROM PollVote v WHERE v.pollId = :pollId")
    long countVotersByPollId(@Param("pollId") Long pollId);

    /**
     * 查询用户参与的所有投票ID（按最近投票时间倒序）
     * 使用 GROUP BY 和 MAX(createdAt) 替代 DISTINCT + ORDER BY，避免 H2 语法限制
     */
    @Query("SELECT v.pollId FROM PollVote v WHERE v.userId = :userId GROUP BY v.pollId ORDER BY MAX(v.createdAt) DESC")
    List<Long> findPollIdsByUserId(@Param("userId") String userId);
}
