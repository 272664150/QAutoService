package com.example.module;

import com.library.core.AutoService;

@AutoService(ITest.class)
public class ThirdTest implements ITest {

    @Override
    public String value() {
        return "Third Test";
    }
}
