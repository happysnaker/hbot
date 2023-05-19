package io.github.happysnaker.hbotcore.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoDispatchActionException extends Exception{
    public NoDispatchActionException(String msg) {
        super(msg);
    }
}
