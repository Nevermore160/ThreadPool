package com.yueqian;

import com.yueqian.ThreadPoolExector;

public interface PolicyHandler {
    void rejected(Runnable task, ThreadPoolExector exector);
}
