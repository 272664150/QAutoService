package com.example.module;

import com.library.core.AutoService;

@AutoService(ITest.class)
public class SecondTest implements ITest {

    @Override
    public String value() {
        return "Second Test";
    }
}