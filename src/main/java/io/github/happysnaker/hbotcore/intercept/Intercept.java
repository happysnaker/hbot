package io.github.happysnaker.hbotcore.intercept;


import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标记该类成为以一个拦截器
 * @author happysnakers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component
public @interface Intercept {
    /**
     * <p><strong>前置拦截方法的调用顺序，order 越高，则越先调用</strong></p>
     */
    int preOrder() default 1;


    /**
     * <p><strong>后置拦截方法的调用顺序，order 越高，则越先调用</strong></p>
     */
    int postOrder() default 1;
}