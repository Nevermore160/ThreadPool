package com.yueqian;

import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolExector implements ExectorService {
    private AtomicInteger ctl = new AtomicInteger(5);//记录当前存活的线程数
    private volatile PolicyHandler handler;
    private int poolSize;
    private final BlockingQueue<Runnable> workQueue;
    private volatile boolean allowThread = false;
    private volatile long keepAliveTime;
    private ReentrantLock mainlock = new ReentrantLock();
    private volatile boolean isShutDown = false;
    private final HashSet<Worker> workers = new HashSet<Worker>();
    private volatile long completedTakCount = 0;


    public ThreadPoolExector(int poolSize, int queueSize, long keepAliveTime, PolicyHandler handler){
        if(poolSize <= 0) throw new IllegalArgumentException("核心线程树不能为空");
        this.poolSize = poolSize;
        this.handler = handler;
        this.keepAliveTime = keepAliveTime;
        if(keepAliveTime > 0) allowThread = true;
        this.workQueue = new ArrayBlockingQueue<Runnable>(5);
    }

    public void execute(Runnable task) {
        if(task == null) throw new NullPointerException("任务不能为空");
        if(isShutDown) throw new IllegalStateException("线程池已经关闭,不接受新的任务");
        int c = ctl.get();
        if(c < poolSize){
            if(addWorker(task,true));
            //创建核心线程接受任务
        }else if(workQueue.offer(task)){

        }else {
            handler.rejected(task,this);//拒绝策略
        }
    }

    public void shutdown() {
        ReentrantLock mainlock = this.mainlock;
        mainlock.lock();
        try {
            isShutDown = true;
            for (Worker worker : workers) {
                Thread t = worker.thread;
                if(!t.isInterrupted() && worker.tryLock()){
                    try {
                        t.interrupt();
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        worker.unlock();
                    }
                }
            }
        }finally {
            mainlock.unlock();
        }
    }

    public int getActiveThread() {
        return ctl.get();
    }

    public Runnable getTask() {
        try {
            return allowThread ? workQueue.poll(keepAliveTime, TimeUnit.SECONDS) : workQueue.take();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return null;
    }

    private boolean addWorker(Runnable r, boolean startNew){
        if(startNew){
            ctl.incrementAndGet();
        }
        boolean workerAdded = false;
        boolean workerStart = false;

        Worker w = new Worker(r);
        Thread t = w.thread;

        if(t != null){
            ReentrantLock mainlock = this.mainlock;
            mainlock.lock();
            try {
                if(!isShutDown){//线程池未关闭
                    if(t.isAlive()) throw new IllegalThreadStateException();
                    workers.add(w);
                    workerAdded = true;
                }
            }finally {
                mainlock.unlock();
            }
            if (workerAdded){
                t.start();
                workerStart = true;
            }
        }
        return workerStart;
    }

    private void runWorker(Worker worker){
        Thread wt = Thread.currentThread();
        Runnable task = worker.firsttask;
        worker.firsttask = null;
        boolean completedAbruptly = true;
        try {
            while (task != null || (task = getTask()) != null){
                worker.lock();
                if(isShutDown && wt.isInterrupted()){
                    wt.interrupt();
                }
                try {
                    task.run();
                }finally {
                    task = null;
                    worker.completedTask ++;
                    worker.unlock();
                }
            }
            completedAbruptly = false;
        }finally {
            processWorkerExit(worker,completedAbruptly);
        }
    }


    public void processWorkerExit(Worker worker, boolean completedAbruptly){
        if(completedAbruptly) ctl.decrementAndGet();

        final ReentrantLock mainlock = this.mainlock;
        mainlock.lock();

        try {
            completedTakCount += worker.completedTask;
            workers.remove(worker);
        }finally {
            mainlock.unlock();
        }
        if(completedAbruptly && !workQueue.isEmpty()){
            addWorker(null,false);
        }
    }

    static AtomicInteger atomic = new AtomicInteger();

    class Worker extends ReentrantLock implements Runnable{
        volatile long completedTask;
        final Thread thread;
        Runnable firsttask;

        public Worker(Runnable r){
            this.firsttask = r;
            this.thread = new Thread(this,"thread-name-" + atomic.incrementAndGet());
        }

        public void run(){
            runWorker(this);
        }

    }
}
