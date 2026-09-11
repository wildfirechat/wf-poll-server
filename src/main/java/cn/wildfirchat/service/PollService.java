package cn.wildfirchat.service;

import cn.wildfirchat.dto.ErrorCode;
import cn.wildfirchat.dto.poll.*;
import cn.wildfirchat.entity.poll.Poll;
import cn.wildfirchat.entity.poll.PollOption;
import cn.wildfirchat.entity.poll.PollVote;
import cn.wildfirchat.exception.BizException;
import cn.wildfirchat.repository.poll.PollOptionRepository;
import cn.wildfirchat.repository.poll.PollRepository;
import cn.wildfirchat.repository.poll.PollVoteRepository;
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.PojoGroupMember;
import cn.wildfirechat.sdk.GroupAdmin;
import cn.wildfirechat.sdk.UserAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PollService {

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollOptionRepository optionRepository;

    @Autowired
    private PollVoteRepository voteRepository;

    @Autowired
    private IMMessageService imMessageService;

    /**
     * 创建投票
     */
    @Transactional
    public Poll createPoll(String creatorId, CreatePollRequest request) {
        // 校验选项数量
        List<String> options = request.getOptions();
        if (options == null || options.size() < 2 || options.size() > 10) {
            throw new BizException(ErrorCode.POLL_INVALID_OPTIONS);
        }

        // 仅群内投票需要检查群成员
        if (request.getVisibility() != null && request.getVisibility() == 1) {
            checkUserInGroup(request.getGroupId(), creatorId);
        }

        // 创建投票
        Poll poll = new Poll();
        poll.setGroupId(request.getGroupId());
        poll.setCreatorId(creatorId);
        poll.setTitle(request.getTitle());
        poll.setDescription(request.getDescription());
        poll.setVisibility(request.getVisibility());
        poll.setType(request.getType());
        poll.setMaxSelect(request.getMaxSelect());
        poll.setAnonymous(request.getAnonymous());
        poll.setEndTime(request.getEndTime());
        // 强制设置为0：投票前隐藏结果（防刷票）
        poll.setShowResult(0);
        poll.setStatus(0);

        pollRepository.save(poll);

        List<PollOption> optionList = new ArrayList<>();
        // 创建选项
        for (int i = 0; i < options.size(); i++) {
            PollOption option = new PollOption();
            option.setPollId(poll.getId());
            option.setOptionText(options.get(i));
            option.setSortOrder(i);
            option.setVoteCount(0);
            optionRepository.save(option);
            optionList.add(option);
        }

        // 仅群内投票发送消息到群聊
        if (poll.getVisibility() != null && poll.getVisibility() == 1 && poll.getGroupId() != null) {
            imMessageService.sendPollMessage(poll, optionList, creatorId);
        }

        return poll;
    }

    /**
     * 获取投票详情
     */
    @Transactional(readOnly = true)
    public PollDetailResponse getPoll(Long pollId, String userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new BizException(ErrorCode.POLL_NOT_FOUND));

        // 检查权限
        checkPollViewPermission(poll, userId);

        // 检查是否已投票
        List<PollVote> userVotes = voteRepository.findByPollIdAndUserId(pollId, userId);
        boolean hasVoted = !userVotes.isEmpty();
        boolean isCreator = poll.getCreatorId().equals(userId);

        // 是否显示结果
        boolean showResult = shouldShowResult(poll, hasVoted, isCreator);

        // 获取选项
        List<PollOption> options = optionRepository.findByPollIdOrderBySortOrderAsc(pollId);
        int totalVotes = options.stream().mapToInt(PollOption::getVoteCount).sum();
        
        // 统计投票人数（去重）
        int voterCount = (int) voteRepository.countVotersByPollId(pollId);

        // 构建响应
        PollDetailResponse response = new PollDetailResponse();
        response.setId(poll.getId());
        response.setGroupId(poll.getGroupId());
        response.setCreatorId(poll.getCreatorId());
        response.setTitle(poll.getTitle());
        response.setDescription(poll.getDescription());
        response.setVisibility(poll.getVisibility());
        response.setType(poll.getType());
        response.setMaxSelect(poll.getMaxSelect());
        response.setAnonymous(poll.getAnonymous());
        response.setEndTime(poll.getEndTime());
        response.setStatus(poll.getStatus());
        response.setShowResult(poll.getShowResult());
        response.setCreatedAt(poll.getCreatedAt());
        response.setUpdatedAt(poll.getUpdatedAt());
        response.setHasVoted(hasVoted);
        response.setIsCreator(isCreator);
        response.setTotalVotes(totalVotes);
        response.setVoterCount(voterCount);

        // 选项列表
        List<PollDetailResponse.PollOptionVO> optionVOs = new ArrayList<>();
        for (PollOption option : options) {
            PollDetailResponse.PollOptionVO vo = new PollDetailResponse.PollOptionVO();
            vo.setId(option.getId());
            vo.setOptionText(option.getOptionText());
            vo.setSortOrder(option.getSortOrder());

            // 显示结果时才返回票数
            if (showResult) {
                vo.setVoteCount(option.getVoteCount());
                if (totalVotes > 0) {
                    vo.setVotePercent((int) (option.getVoteCount() * 100.0 / totalVotes));
                } else {
                    vo.setVotePercent(0);
                }
            }

            optionVOs.add(vo);
        }
        response.setOptions(optionVOs);

        // 我选的选项
        if (hasVoted) {
            response.setMyOptionIds(userVotes.stream().map(PollVote::getOptionId).collect(Collectors.toList()));
        }

        // 投票人详情（仅实名投票且是发起者）
        if (isCreator && poll.getAnonymous() != null && poll.getAnonymous() == 0) {
            List<PollDetailResponse.VoterDetailVO> details = new ArrayList<>();
            List<PollVote> allVotes = voteRepository.findByPollId(pollId);
            
            // 批量获取用户信息
            Map<String, String> userNameCache = new HashMap<>();
            
            for (PollVote vote : allVotes) {
                PollDetailResponse.VoterDetailVO detail = new PollDetailResponse.VoterDetailVO();
                detail.setOptionId(vote.getOptionId());
                detail.setOptionText(getOptionText(options, vote.getOptionId()));
                detail.setUserId(vote.getUserId());
                detail.setCreatedAt(vote.getCreatedAt());
                
                // 获取用户名（带缓存）
                String userName = userNameCache.get(vote.getUserId());
                if (userName == null) {
                    userName = getUserDisplayName(vote.getUserId());
                    userNameCache.put(vote.getUserId(), userName);
                }
                detail.setUserName(userName);
                
                details.add(detail);
            }
            response.setVoterDetails(details);
        }

        return response;
    }

    /**
     * 参与投票
     */
    @Transactional
    public void vote(Long pollId, String userId, VoteRequest request) {
        // 查询并锁定投票
        Poll poll = pollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new BizException(ErrorCode.POLL_NOT_FOUND));

        // 校验投票状态
        validatePollStatus(poll);

        // 检查权限（仅群内投票需要检查）
        checkPollVotePermission(poll, userId);

        // 检查是否已投票（单选/多选都只能投一次）
        boolean hasVoted = voteRepository.existsByPollIdAndUserId(pollId, userId);
        if (hasVoted) {
            throw new BizException(ErrorCode.POLL_ALREADY_VOTED);
        }

        // 校验选项
        List<Long> optionIds = request.getOptionIds();
        if (optionIds == null || optionIds.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }

        // 单选/多选校验
        if (poll.getType() != null && poll.getType() == 1 && optionIds.size() > 1) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
        if (poll.getType() != null && poll.getType() == 2 && poll.getMaxSelect() != null) {
            if (optionIds.size() > poll.getMaxSelect()) {
                throw new BizException(ErrorCode.POLL_MULTI_SELECT_EXCEED);
            }
        }

        // 验证选项是否属于该投票
        List<PollOption> allOptions = optionRepository.findByPollId(pollId);
        List<Long> validOptionIds = allOptions.stream().map(PollOption::getId).collect(Collectors.toList());
        for (Long optionId : optionIds) {
            if (!validOptionIds.contains(optionId)) {
                throw new BizException(ErrorCode.POLL_OPTION_NOT_FOUND);
            }
        }

        // 保存投票记录
        for (Long optionId : optionIds) {
            PollVote vote = new PollVote();
            vote.setPollId(pollId);
            vote.setOptionId(optionId);
            vote.setUserId(userId);
            voteRepository.save(vote);

            // 更新选项票数
            PollOption option = allOptions.stream().filter(o -> o.getId().equals(optionId)).findFirst().orElse(null);
            if (option != null) {
                option.setVoteCount(option.getVoteCount() + 1);
                optionRepository.save(option);
            }
        }
    }

    /**
     * 结束投票
     */
    @Transactional
    public void closePoll(Long pollId, String operatorId) {
        Poll poll = pollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new BizException(ErrorCode.POLL_NOT_FOUND));

        // 仅发起者可结束
        if (!poll.getCreatorId().equals(operatorId)) {
            throw new BizException(ErrorCode.POLL_PERMISSION_DENIED);
        }

        poll.setStatus(1);
        pollRepository.save(poll);

        // 获取最新选项数据（含票数）
        List<PollOption> options = optionRepository.findByPollIdOrderBySortOrderAsc(pollId);

        // 发送结果消息
        imMessageService.sendPollResultMessage(poll, options);
    }

    /**
     * 删除投票（仅创建者）
     */
    @Transactional
    public void deletePoll(Long pollId, String operatorId) {
        Poll poll = pollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new BizException(ErrorCode.POLL_NOT_FOUND));

        // 仅发起者可删除
        if (!poll.getCreatorId().equals(operatorId)) {
            throw new BizException(ErrorCode.POLL_PERMISSION_DENIED);
        }

        // 标记为已删除
        poll.setDeleted(1);
        pollRepository.save(poll);
    }

    /**
     * 导出投票明细
     */
    @Transactional(readOnly = true)
    public List<PollDetailResponse.VoterDetailVO> exportPollDetails(Long pollId, String operatorId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new BizException(ErrorCode.POLL_NOT_FOUND));

        // 仅发起者可导出
        if (!poll.getCreatorId().equals(operatorId)) {
            throw new BizException(ErrorCode.POLL_PERMISSION_DENIED);
        }

        // 匿名投票不能导出
        if (poll.getAnonymous() != null && poll.getAnonymous() == 1) {
            throw new BizException(ErrorCode.POLL_CANNOT_EXPORT);
        }

        List<PollOption> options = optionRepository.findByPollIdOrderBySortOrderAsc(pollId);
        List<PollVote> allVotes = voteRepository.findByPollId(pollId);

        // 批量获取用户信息
        Map<String, String> userNameCache = new HashMap<>();
        
        List<PollDetailResponse.VoterDetailVO> details = new ArrayList<>();
        for (PollVote vote : allVotes) {
            PollDetailResponse.VoterDetailVO detail = new PollDetailResponse.VoterDetailVO();
            detail.setOptionId(vote.getOptionId());
            detail.setOptionText(getOptionText(options, vote.getOptionId()));
            detail.setUserId(vote.getUserId());
            detail.setCreatedAt(vote.getCreatedAt());
            
            // 获取用户名（带缓存）
            String userName = userNameCache.get(vote.getUserId());
            if (userName == null) {
                userName = getUserDisplayName(vote.getUserId());
                userNameCache.put(vote.getUserId(), userName);
            }
            detail.setUserName(userName);
            
            details.add(detail);
        }

        return details;
    }

    /**
     * 获取用户显示名称
     */
    private String getUserDisplayName(String userId) {
        try {
            IMResult<InputOutputUserInfo> result = UserAdmin.getUserByUserId(userId);
            if (result != null && result.getErrorCode() == cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS) {
                InputOutputUserInfo userInfo = result.getResult();
                if (userInfo.getDisplayName() != null && !userInfo.getDisplayName().isEmpty()) {
                    return userInfo.getDisplayName();
                } else if (userInfo.getName() != null && !userInfo.getName().isEmpty()) {
                    return userInfo.getName();
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败, userId: {}, error: {}", userId, e.getMessage());
        }
        return userId; // 默认返回用户ID
    }

    /**
     * 校验投票状态
     */
    private void validatePollStatus(Poll poll) {
        if (poll.getStatus() != null && poll.getStatus() == 1) {
            throw new BizException(ErrorCode.POLL_ALREADY_ENDED);
        }
        if (poll.getEndTime() > 0 && poll.getEndTime() < System.currentTimeMillis()) {
            throw new BizException(ErrorCode.POLL_EXPIRED);
        }
    }

    /**
     * 检查查看权限
     */
    private void checkPollViewPermission(Poll poll, String userId) {
        // 仅群内投票需要检查是否在群内
        if (poll.getVisibility() != null && poll.getVisibility() == 1 && poll.getGroupId() != null) {
            checkUserInGroup(poll.getGroupId(), userId);
        }
        // 公开投票：任何人可查看
    }

    /**
     * 检查投票权限
     */
    private void checkPollVotePermission(Poll poll, String userId) {
        // 仅群内投票需要检查群成员
        if (poll.getVisibility() != null && poll.getVisibility() == 1 && poll.getGroupId() != null) {
            checkUserInGroup(poll.getGroupId(), userId);
        }
        // 公开投票：任何人可投票
    }

    /**
     * 判断是否显示结果
     * 防刷票：投票前所有人不可见，投票后/结束后可见
     */
    private boolean shouldShowResult(Poll poll, boolean hasVoted, boolean isCreator) {
        // 投票已结束，所有人可见
        if (poll.getStatus() != null && poll.getStatus() == 1) {
            return true;
        }
        // 已投票者可见
        if (hasVoted) {
            return true;
        }
        return false;
    }

    /**
     * 获取选项文本
     */
    private String getOptionText(List<PollOption> options, Long optionId) {
        return options.stream()
                .filter(o -> o.getId().equals(optionId))
                .map(PollOption::getOptionText)
                .findFirst()
                .orElse("");
    }

    /**
     * 检查用户是否在群组中（复用投票逻辑）
     */
    private void checkUserInGroup(String groupId, String userId) {
        int maxRetries = 2;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxRetries) {
            try {
                IMResult<PojoGroupMember> result = GroupAdmin.getGroupMember(groupId, userId);

                if (result != null && result.getErrorCode() == cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS) {
                    PojoGroupMember member = result.getResult();
                    if (member == null || member.getType() == 4) {
                        log.warn("用户已被移除群组, groupId: {}, userId: {}", groupId, userId);
                        throw new BizException(ErrorCode.POLL_NOT_IN_GROUP);
                    }
                    return;
                }

                if (result != null) {
                    cn.wildfirechat.common.ErrorCode errorCode = result.getErrorCode();
                    if (errorCode == cn.wildfirechat.common.ErrorCode.ERROR_CODE_NOT_EXIST ||
                        errorCode == cn.wildfirechat.common.ErrorCode.ERROR_CODE_NOT_IN_GROUP) {
                        log.warn("用户不在群组中, groupId: {}, userId: {}, errorCode: {}",
                                groupId, userId, errorCode);
                        throw new BizException(ErrorCode.POLL_NOT_IN_GROUP);
                    }
                    log.warn("IM服务返回业务错误, groupId: {}, userId: {}, errorCode: {}",
                            groupId, userId, errorCode);
                    return;
                }

                log.warn("IM服务返回空结果, 准备重试, groupId: {}, userId: {}, retry: {}/{}",
                        groupId, userId, retryCount + 1, maxRetries);

            } catch (BizException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                log.warn("调用IM服务发生异常, 准备重试, groupId: {}, userId: {}, retry: {}/{}， error: {}",
                        groupId, userId, retryCount + 1, maxRetries, e.getMessage());
            }

            retryCount++;
            if (retryCount <= maxRetries) {
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("IM服务检查群组失败已达最大重试次数，采取降级策略允许操作继续, groupId: {}, userId: {}",
                groupId, userId, lastException);
    }

    /**
     * 获取用户的投票列表（创建的 + 参与的）
     */
    @Transactional(readOnly = true)
    public List<PollListItemResponse> getUserPolls(String userId, String groupId) {
        List<PollListItemResponse> result = new ArrayList<>();
        
        // 获取用户创建的投票（过滤已删除的）
        List<Poll> createdPolls = pollRepository.findByCreatorIdOrderByCreatedAtDesc(userId);
        for (Poll poll : createdPolls) {
            // 跳过已删除的投票
            if (poll.getDeleted() != null && poll.getDeleted() == 1) {
                continue;
            }
            // 如果指定了群组，只返回该群组的投票
            if (groupId != null && !groupId.equals(poll.getGroupId())) {
                continue;
            }
            result.add(convertToListItem(poll, userId, true));
        }
        
        return result;
    }
    
    /**
     * 转换为列表项
     */
    private PollListItemResponse convertToListItem(Poll poll, String userId, boolean isCreator) {
        PollListItemResponse item = new PollListItemResponse();
        item.setId(poll.getId());
        item.setGroupId(poll.getGroupId());
        item.setCreatorId(poll.getCreatorId());
        item.setTitle(poll.getTitle());
        item.setType(poll.getType());
        item.setAnonymous(poll.getAnonymous());
        item.setStatus(poll.getStatus());
        item.setEndTime(poll.getEndTime());
        item.setCreatedAt(poll.getCreatedAt());
        item.setCreator(Boolean.valueOf(isCreator));
        item.setIsCreator(Boolean.valueOf(isCreator));
        
        // 获取总票数
        List<PollOption> options = optionRepository.findByPollIdOrderBySortOrderAsc(poll.getId());
        int totalVotes = options.stream().mapToInt(PollOption::getVoteCount).sum();
        item.setTotalVotes(totalVotes);
        
        // 获取投票人数（去重）
        int voterCount = (int) voteRepository.countVotersByPollId(poll.getId());
        item.setVoterCount(voterCount);
        
        // 检查当前用户是否已投票
        List<PollVote> userVotes = voteRepository.findByPollIdAndUserId(poll.getId(), userId);
        item.setHasVoted(!userVotes.isEmpty());
        
        // 检查是否已过期
        boolean isExpired = poll.getEndTime() > 0 && poll.getEndTime() < System.currentTimeMillis();
        item.setExpired(isExpired);
        
        // 设置删除标记
        boolean isDeleted = poll.getDeleted() != null && poll.getDeleted() == 1;
        item.setDeleted(isDeleted);
        
        return item;
    }
}
