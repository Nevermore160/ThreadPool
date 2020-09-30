package com.yueqian;

public interface ExectorService {
    public void execute(Runnable task);

    public void shutdown();

    public int getActiveThread();

    Runnable getTask();

}
