package com.yeschillaicode.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 表示该注解只能用于方法上
@Target(ElementType.METHOD)
// 表示该注解在运行时仍然保留，可以通过反射获取
@Retention(RetentionPolicy.RUNTIME)


/**
 * 自定义注解，用于进行权限检查
 * 通过在方法上添加此注解，可以指定调用该方法所需的用户角色
 */
public @interface AuthCheck {
    /**
     * 定义一个名为mustRole的属性，用于指定必须的角色
     * 默认值为空字符串，表示没有特殊角色要求
     * @return 返回必须的角色名称
     */
    String mustRole() default "";
}
