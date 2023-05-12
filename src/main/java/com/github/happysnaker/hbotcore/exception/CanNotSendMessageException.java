package com.github.happysnaker.hbotcore.exception;

/**
 * 当消息发送失败时抛出此异常
 * @author Happysnaker
 * @description
 * @date 2022/1/14
 * @email happysnaker@foxmail.com
 */
public class CanNotSendMessageException extends Exception {
    public CanNotSendMessageException(String message) {
        super(message);
    }
}
