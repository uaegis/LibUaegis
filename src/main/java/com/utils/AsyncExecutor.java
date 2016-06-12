package com.utils;

/**
 * Created by Lehyu on 2016/5/22.
 */
public abstract interface AsyncExecutor<T> {
    public abstract T asyncExecute();
    public abstract void executeComplete(T obj);
    public abstract void executePrepare();
}
