package com.yeschillaicode.aop;

import com.yeschillaicode.annotation.AuthCheck;
import com.yeschillaicode.exception.BusinessException;
import com.yeschillaicode.exception.ErrorCode;
import com.yeschillaicode.model.entity.User;
import com.yeschillaicode.model.enums.UserRoleEnum;
import com.yeschillaicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect // 声明这是一个切面类
@Component // 将此类声明为Spring组件，使其被Spring容器管理

public class AuthInterceptor {
    @Resource // 自动注入UserService实例
    private UserService userService;


    /**
     * 环绕通知，用于方法执行前进行权限检查
     * @param joinPoint 切入点，可以获取目标方法的信息
     * @param authCheck 权限校验注解
     * @return 目标方法的执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("@annotation(authCheck)") // 环绕通知，拦截带有AuthCheck注解的方法
    public  Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        //从请求中获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //获取当前必须要拥有的权限
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //当前必须要拥有的权限为空，说明不需要权限，直接放行
        if (mustRoleEnum == null){
            return joinPoint.proceed();
        }
      //以下的代码，说明必须要有这个权限才能通过
      //获得当前登录的用户有什么权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        //如果当前用户拥有的权限为空，说明没有权限，拒绝通过
        if (userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"没有权限");
        }
        //要求需要管理员权限，但是当前登录的用户没有管理员权限
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限");
        }
        //到这说明当前用户权限是普通用户，放行
        return joinPoint.proceed();
    }
}
