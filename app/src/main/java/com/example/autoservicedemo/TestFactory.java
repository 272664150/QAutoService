package com.example.autoservicedemo;

import com.module.base.ITest;

import java.util.Iterator;
import java.util.ServiceLoader;

public class TestFactory {

    private static volatile TestFactory mTestFactory;

    private Iterator<ITest> mIterator;

    private TestFactory() {
        ServiceLoader<ITest> loader = ServiceLoader.load(ITest.class);
        mIterator = loader.iterator();
    }

    public static TestFactory getSingleton() {
        if (null == mTestFactory) {
            synchronized (TestFactory.class) {
                if (null == mTestFactory) {
                    mTestFactory = new TestFactory();
                }
            }
        }
        return mTestFactory;
    }

    public ITest getValue() {
        return mIterator.next();
    }

    public boolean hasNextValue() {
        return mIterator.hasNext();
    }
}