package cn.wildfirchat.repository.poll;

import cn.wildfirchat.entity.poll.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollIdOrderBySortOrderAsc(Long pollId);

    List<PollOption> findByPollId(Long pollId);
}
