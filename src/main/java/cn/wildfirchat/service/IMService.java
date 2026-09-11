package cn.wildfirchat.service;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.Conversation;
import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.sdk.MessageAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class IMService {

    /**
     * 发送群组消息
     *
     * @param fromUser 发送者用户ID
     * @param groupId  群组ID
     * @param msgType  消息类型
     * @param content  消息内容（JSON字符串）
     * @return 是否发送成功
     */
    public boolean sendGroupMessage(String fromUser, String groupId, int msgType, String content) {
        try {
            // 创建群组会话
            Conversation conversation = new Conversation();
            conversation.setType(1); // 1 = 群组会话
            conversation.setTarget(groupId);
            conversation.setLine(0);

            // 创建消息payload
            MessagePayload payload = new MessagePayload();
            payload.setType(msgType);
            payload.setBase64edData(Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            payload.setPersistFlag(3);

            IMResult<cn.wildfirechat.pojos.SendMessageResult> result = 
                    MessageAdmin.sendMessage(fromUser, conversation, payload);

            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                log.debug("发送群组消息成功, from: {}, to: {}, type: {}", fromUser, groupId, msgType);
                return true;
            } else {
                log.error("发送群组消息失败, from: {}, to: {}, type: {}, error: {}",
                        fromUser, groupId, msgType,
                        result != null ? result.getErrorCode() : "null");
                return false;
            }
        } catch (Exception e) {
            log.error("发送群组消息异常, from: {}, to: {}, type: {}", fromUser, groupId, msgType, e);
            return false;
        }
    }

    /**
     * 发送单聊消息
     *
     * @param fromUser 发送者用户ID
     * @param toUser   接收者用户ID
     * @param msgType  消息类型
     * @param content  消息内容（JSON字符串）
     * @return 是否发送成功
     */
    public boolean sendSingleMessage(String fromUser, String toUser, int msgType, String content) {
        try {
            // 创建单聊会话
            Conversation conversation = new Conversation();
            conversation.setType(0); // 0 = 单聊会话
            conversation.setTarget(toUser);
            conversation.setLine(0);

            // 创建消息payload
            MessagePayload payload = new MessagePayload();
            payload.setType(msgType);
            payload.setBase64edData(Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            payload.setPersistFlag(3);

            IMResult<cn.wildfirechat.pojos.SendMessageResult> result = 
                    MessageAdmin.sendMessage(fromUser, conversation, payload);

            if (result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                log.debug("发送单聊消息成功, from: {}, to: {}, type: {}", fromUser, toUser, msgType);
                return true;
            } else {
                log.error("发送单聊消息失败, from: {}, to: {}, type: {}, error: {}",
                        fromUser, toUser, msgType,
                        result != null ? result.getErrorCode() : "null");
                return false;
            }
        } catch (Exception e) {
            log.error("发送单聊消息异常, from: {}, to: {}, type: {}", fromUser, toUser, msgType, e);
            return false;
        }
    }
}
