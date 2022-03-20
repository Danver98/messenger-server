package com.danver.messengerserver.configs;


import com.danver.messengerserver.filters.UserAuthenticationFilter;
import com.danver.messengerserver.filters.UserAuthorizationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    FilterRegistrationBean<UserAuthenticationFilter> userAuthenticationFilterReg(UserAuthenticationFilter userAuthenticationFilter) {
        FilterRegistrationBean<UserAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(userAuthenticationFilter);
        registrationBean.addUrlPatterns("/users/*");
        return registrationBean;
    }

    @Bean
    FilterRegistrationBean<UserAuthorizationFilter> userAuthorizationFilterReg(UserAuthorizationFilter userAuthorizationFilter) {
        FilterRegistrationBean<UserAuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(userAuthorizationFilter);
        registrationBean.addUrlPatterns("/users/*");
        return registrationBean;
    }

}
