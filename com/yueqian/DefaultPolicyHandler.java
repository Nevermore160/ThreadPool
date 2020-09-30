package com.yueqian;

public class DefaultPolicyHandler implements PolicyHandler{

    public DefaultPolicyHandler(){}

    public void rejected(Runnable task, ThreadPoolExector exector) {
        System.out.println("任务已经满了");
        throw new PolicyException("任务已经满了");
    }
}
