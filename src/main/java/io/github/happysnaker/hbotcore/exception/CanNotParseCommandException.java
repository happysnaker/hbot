package io.github.happysnaker.hbotcore.exception;

/**
 * 当命令解析出错时抛出此异常
 * @author Happysnaker
 * @description
 * @date 2022/2/23
 * @email happysnaker@foxmail.com
 */
public class CanNotParseCommandException extends Exception {
    public CanNotParseCommandException() {
        super();
    }

    public CanNotParseCommandException(String message) {
        super(message);
    }

    public CanNotParseCommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CanNotParseCommandException(Throwable cause) {
        super(cause);
    }

    protected CanNotParseCommandException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
