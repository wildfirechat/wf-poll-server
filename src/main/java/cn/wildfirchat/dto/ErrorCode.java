package cn.wildfirchat.dto;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 成功
    SUCCESS(0, "success"),

    // 系统错误 1-999
    SYSTEM_ERROR(1, "系统错误"),
    PARAM_ERROR(2, "参数错误"),
    UNKNOWN_ERROR(999, "未知错误"),

    // 认证错误 1000-1999
    UNAUTHORIZED(1000, "未授权"),
    AUTH_CODE_MISSING(1001, "缺少authCode"),
    AUTH_CODE_INVALID(1002, "无效的authCode"),

    // 投票错误 4000-4999
    POLL_NOT_FOUND(4000, "投票不存在"),
    POLL_ALREADY_ENDED(4001, "投票已结束"),
    POLL_ALREADY_VOTED(4002, "已经投过票了"),
    POLL_OPTION_NOT_FOUND(4003, "选项不存在"),
    POLL_MULTI_SELECT_EXCEED(4004, "选择数量超出限制"),
    POLL_NOT_IN_GROUP(4005, "仅群成员可参与此投票"),
    POLL_PERMISSION_DENIED(4006, "无权操作此投票"),
    POLL_INVALID_OPTIONS(4007, "选项数量错误（2-10个）"),
    POLL_CANNOT_EXPORT(4008, "匿名投票无法导出明细"),
    POLL_EXPIRED(4009, "投票已过期");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
