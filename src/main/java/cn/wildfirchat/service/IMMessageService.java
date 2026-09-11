package cn.wildfirchat.service;

import cn.wildfirchat.entity.poll.Poll;
import cn.wildfirchat.entity.poll.PollOption;
import cn.wildfirchat.repository.poll.PollVoteRepository;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IMMessageService {

    @Value("${poll.notification.user_id:system}")
    private String notificationUserId;

    @Autowired
    private IMService imService;

    @Autowired
    private PollVoteRepository voteRepository;

    /**
     * 发送投票创建消息
     */
    private static final Gson gson = new Gson();
    
    public void sendPollMessage(Poll poll, List<PollOption> options, String operatorId) {
        // 使用创建人身份发送投票消息
        String actualSenderId = poll.getCreatorId();

        Map<String, Object> content = new HashMap<>();
        content.put("pollId", String.valueOf(poll.getId()));
        content.put("groupId", poll.getGroupId());
        content.put("creatorId", poll.getCreatorId());
        content.put("title", poll.getTitle());
        content.put("desc", poll.getDescription());
        content.put("visibility", poll.getVisibility());
        content.put("type", poll.getType());
        content.put("anonymous", poll.getAnonymous());
        content.put("endTime", poll.getEndTime());
        content.put("status", poll.getStatus());
        content.put("totalVotes", options.stream().mapToInt(PollOption::getVoteCount).sum());

        String jsonContent = gson.toJson(content);

        // 使用 MessageAdmin.sendMessage 发送消息
        imService.sendGroupMessage(actualSenderId, poll.getGroupId(), 18, jsonContent);

        log.info("投票创建消息已发送, pollId: {}, groupId: {}, sender: {}",
                poll.getId(), poll.getGroupId(), actualSenderId);
    }

    /**
     * 发送投票结果消息
     * 1. 发送单聊通知给创建者
     * 2. 如果是群组内投票，发送群消息通知群成员
     */
    public void sendPollResultMessage(Poll poll, List<PollOption> options) {
        // 使用配置身份发送结果
        String actualSenderId = getNotificationUserId();

        // 找出最高票数
        int maxVotes = options.stream().mapToInt(PollOption::getVoteCount).max().orElse(0);
        if(maxVotes == 0) {
            // 没有投票，不发送通知
            return;
        }
        List<PollOption> winningOptions = options.stream()
                .filter(o -> o.getVoteCount() == maxVotes && maxVotes > 0)
                .collect(Collectors.toList());

        int totalVotes = options.stream().mapToInt(PollOption::getVoteCount).sum();
        int voterCount = (int) voteRepository.countVotersByPollId(poll.getId());

        Map<String, Object> content = new HashMap<>();
        content.put("pollId", poll.getId());
        content.put("title", poll.getTitle());
        content.put("status", 1);
        content.put("creatorId", poll.getCreatorId());
        content.put("totalVotes", totalVotes);
        content.put("voterCount", voterCount);

        List<Map<String, Object>> resultList = options.stream().map(o -> {
            Map<String, Object> item = new HashMap<>();
            item.put("optionId", o.getId());
            item.put("optionText", o.getOptionText());
            item.put("voteCount", o.getVoteCount());
            return item;
        }).collect(Collectors.toList());
        content.put("results", resultList);

        if (!winningOptions.isEmpty()) {
            List<String> winnerIds = winningOptions.stream()
                    .map(o -> String.valueOf(o.getId()))
                    .collect(Collectors.toList());
            List<String> winnerTexts = winningOptions.stream()
                    .map(PollOption::getOptionText)
                    .collect(Collectors.toList());
            content.put("winningOptionIds", winnerIds);
            content.put("winningOptionTexts", winnerTexts);
        }
        content.put("endedAt", System.currentTimeMillis());

        String jsonContent = gson.toJson(content);

        // 1. 首先发送单聊通知给创建者
        imService.sendSingleMessage(actualSenderId, poll.getCreatorId(), 19, jsonContent);
        log.info("投票结果单聊消息已发送给创建者, pollId: {}, creatorId: {}, sender: {}",
                poll.getId(), poll.getCreatorId(), actualSenderId);

        // 2. 如果是群组内投票，还需要发送群消息通知群成员
        if (poll.getVisibility() != null && poll.getVisibility() == 1 && poll.getGroupId() != null) {
            imService.sendGroupMessage(actualSenderId, poll.getGroupId(), 19, jsonContent);
            log.info("投票结果群消息已发送, pollId: {}, groupId: {}, sender: {}",
                    poll.getId(), poll.getGroupId(), actualSenderId);
        }
    }

    /**
     * 获取通知用户ID
     */
    private String getNotificationUserId() {
        return notificationUserId != null && !notificationUserId.trim().isEmpty()
                ? notificationUserId.trim()
                : "system";
    }
}
