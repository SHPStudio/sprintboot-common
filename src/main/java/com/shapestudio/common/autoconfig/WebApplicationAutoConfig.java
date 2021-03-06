package com.shapestudio.common.autoconfig;

import com.google.common.collect.Lists;
import com.shapestudio.common.exception.GlobalExceptionResolver;
import com.shapestudio.common.filter.RequestFilter;
import com.shapestudio.common.interceptor.CorsInterceptor;
import com.shapestudio.common.interceptor.DefaultCorsInterceptor;
import com.shapestudio.common.interceptor.LoginInterceptor;
import com.shapestudio.common.util.ApplicationContextUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

/**
 * Web项目自动配置
 */
@Configuration
@ConditionalOnWebApplication
public class WebApplicationAutoConfig {
    /**
     * 配置文件url分隔符
     */
    private final static String CONFIG_FILE_URL_SPLIT_MARK = ",";

    /**
     * web mvc相关配置
     */
    @Configuration
    public static class WebMvcAutoConfig implements WebMvcConfigurer, ApplicationContextAware {
        private ApplicationContext applicationContext;

        /**
         * 是否开启Cors
         */
        @Value("${shape.cors.enable:false}")
        private boolean enableCors;

        /**
         * 登录拦截排除的url
         */
        @Value("${shape.except.login.urls:}")
        private String exceptLoginInterceptUrls;

        /**
         * Cors需要排除的url
         */
        @Value("${shape.except.cors.urls:}")
        private String exceptCorsUrls;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            // 1.检查loginInterceptor
            LoginInterceptor loginInterceptor = getBeanFromApplicationContext(LoginInterceptor.class);
            if (null != loginInterceptor) {
                registry.addInterceptor(loginInterceptor).addPathPatterns("/**").excludePathPatterns(Optional.ofNullable(exceptLoginInterceptUrls)
                    .filter(StringUtils::isNoneBlank).map(str -> Lists.newArrayList(str.split(CONFIG_FILE_URL_SPLIT_MARK))).orElse(Lists.newArrayList()));
            }

            // 2.是否开启了cors
            if (enableCors) {
                // 3.检查corsInterceptor
                CorsInterceptor corsInterceptor = getBeanFromApplicationContext(CorsInterceptor.class);
                if (null == corsInterceptor) {
                    corsInterceptor = new DefaultCorsInterceptor();
                }
                registry.addInterceptor(corsInterceptor).addPathPatterns("/**").excludePathPatterns(Optional.ofNullable(exceptCorsUrls)
                        .filter(StringUtils::isNoneBlank).map(str -> Lists.newArrayList(str.split(CONFIG_FILE_URL_SPLIT_MARK))).orElse(Lists.newArrayList()));
            }
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
        }

        /**
         * 从ApplicationContext中根据特定类返回bean
         * @param targetClass
         * @param <T>
         * @return
         */
        private <T> T getBeanFromApplicationContext(Class<T> targetClass) {
            try {
                return applicationContext.getBean(targetClass);
            }catch (Exception e) {
                // ignore
                return null;
            }

        }

//        /**
//         * 增加自定义异常处理
//         * @param resolvers
//         */
//        @Override
//        public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
//            resolvers.add(new GlobalExceptionResolver());
//        }
    }

    /**
     * 使用filter解决request的流只能读取一次的问题
     * @return
     */
    @Bean
    public FilterRegistrationBean httpServletRequestReplacedRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RequestFilter());
        registration.addUrlPatterns("/*");
        registration.addInitParameter("paramName", "paramValue");
        registration.setName("httpServletRequestReplacedFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 通用错误处理
     * @return
     */
    @Bean
    public DefaultErrorAttributes globalExceptionResolver() {
        return new GlobalExceptionResolver();
    }

    /**
     * application工具类
     * @return
     */
    @Bean
    public ApplicationContextUtil applicationContextUtil() {
        return new ApplicationContextUtil();
    }

    /**
     * 探活url
     */
    @RestController
    public static class HealthController {
        @RequestMapping("/health")
        public String health() {
            return "ok";
        }
    }
}
