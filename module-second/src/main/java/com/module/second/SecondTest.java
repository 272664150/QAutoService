package com.module.second;

import com.module.base.ITest;

public class SecondTest implements ITest {

    @Override
    public String value() {
        return "Second Test";
    }
}