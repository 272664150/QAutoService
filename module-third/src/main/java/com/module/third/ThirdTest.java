package com.module.third;

import com.service.annotations.AutoService;
import com.module.base.ITest;

@AutoService(ITest.class)
public class ThirdTest implements ITest {

    @Override
    public String value() {
        return "Third Test";
    }
}
