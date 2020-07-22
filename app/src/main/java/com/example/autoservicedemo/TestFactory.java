package com.example.autoservicedemo;

import com.example.module.ITest;
import com.library.core.QServiceLoader;

import java.util.Iterator;

public class TestFactory {

    private static volatile TestFactory mTestFactory;

    private Iterator<ITest> mIterator;

    private TestFactory() {
        QServiceLoader<ITest> loader = QServiceLoader.load(ITest.class);
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