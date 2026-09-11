package cn.wildfirchat.filter;

import cn.wildfirechat.pojos.OutputApplicationUserInfo;
import cn.wildfirechat.sdk.UserAdmin;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirchat.dto.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class AuthFilter implements Filter {

    public static final String USER_ID_KEY = "userId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if(((HttpServletRequest) request).getMethod().equals("OPTIONS")){
            chain.doFilter(request, response);
            return;
        }

        // 获取请求路径
        String requestURI = httpRequest.getRequestURI();
        log.debug("AuthFilter processing request: {}", requestURI);

        // 从header中获取authCode
        String authCode = httpRequest.getHeader("authCode");
        if (authCode == null || authCode.isEmpty()) {
            log.warn("Missing authCode in request: {}", requestURI);
            writeErrorResponse(httpResponse, ErrorCode.AUTH_CODE_MISSING);
            return;
        }

        // 验证authCode并获取用户信息
        String userId = validateAuthCode(authCode);
        if (userId == null) {
            log.warn("Invalid authCode: {}", authCode);
            writeErrorResponse(httpResponse, ErrorCode.AUTH_CODE_INVALID);
            return;
        }

        // 将用户信息设置到request属性中
        httpRequest.setAttribute(USER_ID_KEY, userId);

        log.debug("Auth success, userId: {}", userId);

        chain.doFilter(request, response);
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":%d,\"message\":\"%s\"}",
                errorCode.getCode(), errorCode.getMessage()));
    }

    /**
     * 验证authCode，返回用户信息
     */
    private String validateAuthCode(String authCode) {
        log.info("Validate authCode with IM service, authCode: {}", authCode);
        try {
            IMResult<OutputApplicationUserInfo> imResult = UserAdmin.applicationGetUserInfo(authCode);
            if (imResult != null && imResult.getErrorCode() == cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS) {
                return imResult.getResult().getUserId();
            }
        } catch (Exception e) {
            log.error("Failed to validate authCode", e);
        }
        return null;
    }
}
