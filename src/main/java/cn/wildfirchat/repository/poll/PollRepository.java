package cn.wildfirchat.repository.poll;

import cn.wildfirchat.entity.poll.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Poll p WHERE p.id = :id")
    Optional<Poll> findByIdForUpdate(@Param("id") Long id);

    /**
     * 查询用户创建的投票列表
     */
    List<Poll> findByCreatorIdOrderByCreatedAtDesc(String creatorId);

    /**
     * 查询群组的投票列表
     */
    List<Poll> findByGroupIdOrderByCreatedAtDesc(String groupId);
}
