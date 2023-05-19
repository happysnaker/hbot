package io.github.happysnaker.hbotcore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Test {

    @Autowired
    TestHandler handler;


    @org.junit.jupiter.api.Test
    public void test() {
        handler.m0(null);
    }
}
