package com.mall.security.aspect;

import com.mall.security.annotation.DataScope;
import com.mall.security.user.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class DataScopeAspect {

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            DataScopeContext.clear();
            return;
        }

        LoginUser loginUser = (LoginUser) auth.getPrincipal();
        DataScopeContext.setScopeSql(buildScopeSql(loginUser, dataScope));
        log.debug("DataScope set for user {}: {}", loginUser.getUsername(), DataScopeContext.getScopeSql());
    }

    private String buildScopeSql(LoginUser user, DataScope dataScope) {
        // Store manager sees all, no filter
        if (user.getPermissions().contains("*")) {
            return "";
        }
        return "";
    }
}