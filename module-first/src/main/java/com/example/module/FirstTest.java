package com.example.module;

import com.library.core.AutoService;

@AutoService(ITest.class)
public class FirstTest implements ITest {

    @Override
    public String value() {
        return "First Test";
    }
}