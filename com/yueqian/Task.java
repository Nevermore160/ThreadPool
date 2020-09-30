package com.yueqian;

public class Task implements Runnable{
    private int nov;

    public Task(int i){
        this.nov = i;
    }

    public void run() {
        System.out.println("执行当前线程:" + Thread.currentThread().getName());
        System.out.println("我是任务:" + nov + "我在执行...");
    }
}
