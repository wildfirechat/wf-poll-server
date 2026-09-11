package cn.wildfirchat.config;

import cn.wildfirchat.filter.AuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<AuthFilter> authFilterRegistration(AuthFilter authFilter) {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authFilter);
        registration.addUrlPatterns("/api/*");  // 拦截所有 /api 路径
        registration.setOrder(1);  // 设置过滤器顺序，数字越小优先级越高
        registration.setName("authFilter");
        return registration;
    }
}
