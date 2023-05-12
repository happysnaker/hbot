package io.github.happysnaker.hbotcore.exception;

/**
 * 当操作权限不足时抛出此异常
 * @author Happysnaker
 * @description
 * @date 2022/1/29
 * @email happysnaker@foxmail.com
 */
public class InsufficientPermissionsException extends Exception {

    public InsufficientPermissionsException() {
    }

    public InsufficientPermissionsException(String message) {
        super(message);
    }
}
