package com.utils;

/**
 * Created by Lehyu on 2016/5/22.
 */
public abstract interface MessageListener<T> {
    public abstract void handleMessage(int option, T obj);
}
