package com.module.first;

import com.service.annotations.AutoService;
import com.module.base.ITest;

@AutoService(ITest.class)
public class FirstTest implements ITest {

    @Override
    public String value() {
        return "First Test";
    }
}