package io.github.happysnaker.hbotcore.permisson;

import java.lang.annotation.*;

/**
 * 注释在方法上，标识方法必须需要某个权限方能调用
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface Permission {
    int value();
}
