package com.library.core;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class QServiceLoader<T> implements Iterable<T> {

    private final ServiceLoader<T> mLoader;

    private QServiceLoader(Class<T> svc) {
        if (svc == null) {
            throw new NullPointerException("Service interface cannot be null");
        }
        mLoader = ServiceLoader.load(svc);
    }

    public static <T> QServiceLoader<T> load(Class<T> clazz) {
        return new QServiceLoader<>(clazz);
    }

    @Override
    public Iterator<T> iterator() {
        return mLoader.iterator();
    }
}
