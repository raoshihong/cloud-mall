package com.rao.cloud.mall.common.utils;

import org.apache.shiro.authz.AuthorizationException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public class AuthenticatingFilterConfig implements ApplicationContextAware {
    private ApplicationContext context;

    public Boolean hasNoAuthentication(HttpServletRequest request){
        Collection<RequestMappingHandlerMapping > collections=context.getBeansOfType(RequestMappingHandlerMapping.class).values();
        try {
            RequestMappingHandlerMapping requestMappingHandlerMapping = collections.stream().findFirst().get();
            HandlerExecutionChain handler = requestMappingHandlerMapping.getHandler(request);
            HandlerMethod obj = (HandlerMethod) handler.getHandler();
            return AnnotatedElementUtils.isAnnotated(obj.getMethod(), NoAuthentication.class);
        } catch (Exception e) {
            throw new AuthorizationException("无效访问路径");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
