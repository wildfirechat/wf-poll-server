package cn.wildfirchat.controller;

import cn.wildfirchat.dto.Result;
import cn.wildfirchat.dto.poll.*;
import cn.wildfirchat.entity.poll.Poll;
import cn.wildfirchat.filter.AuthFilter;
import cn.wildfirchat.service.PollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/polls")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PollController {

    @Autowired
    private PollService pollService;

    /**
     * 创建投票
     */
    @PostMapping
    public Result<Poll> create(@RequestBody CreatePollRequest request,
                                HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        Poll poll = pollService.createPoll(userId, request);
        return Result.success(poll);
    }

    /**
     * 获取投票详情
     */
    @PostMapping("/{pollId}")
    public Result<PollDetailResponse> getPoll(@PathVariable Long pollId,
                                               HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        PollDetailResponse detail = pollService.getPoll(pollId, userId);
        return Result.success(detail);
    }

    /**
     * 获取用户的投票列表（创建的 + 参与的）
     */
    @PostMapping("/my")
    public Result<List<PollListItemResponse>> getMyPolls(HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        List<PollListItemResponse> polls = pollService.getUserPolls(userId, null);
        return Result.success(polls);
    }

    /**
     * 参与投票
     */
    @PostMapping("/{pollId}/vote")
    public Result<Void> vote(@PathVariable Long pollId,
                              @RequestBody VoteRequest request,
                              HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        pollService.vote(pollId, userId, request);
        return Result.success();
    }

    /**
     * 结束投票
     */
    @PostMapping("/{pollId}/close")
    public Result<Void> close(@PathVariable Long pollId,
                               HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        pollService.closePoll(pollId, userId);
        return Result.success();
    }

    /**
     * 导出投票明细
     */
    @PostMapping("/{pollId}/export")
    public Result<List<PollDetailResponse.VoterDetailVO>> exportDetails(@PathVariable Long pollId,
                                                                         HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        List<PollDetailResponse.VoterDetailVO> details = pollService.exportPollDetails(pollId, userId);
        return Result.success(details);
    }

    /**
     * 删除投票（仅创建者）
     */
    @PostMapping("/{pollId}/delete")
    public Result<Void> delete(@PathVariable Long pollId,
                                HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute(AuthFilter.USER_ID_KEY);
        pollService.deletePoll(pollId, userId);
        return Result.success();
    }
}
