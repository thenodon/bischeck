package com.ingby.socbox.bischeck.servers.queue;


import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class BatchAndTimeQueue<E> implements Runnable {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(BatchAndTimeQueue.class);
    
    private CallBack callback;
    private Integer size;
    private Integer timeout;
    private ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<>();
    
    Thread t = new Thread("Timer");
    private ExecutorService pool;
    private AtomicBoolean started = new AtomicBoolean(false);
    
    public static void main(String[] args) {
        CallBack callback = new CallBackTestImpl();
        BatchAndTimeQueue<String> b = new BatchAndTimeQueue<>(callback, 10, 3000);
        int count = 0;
        while (true) {
            b.put("Hello" + count++);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {}
        }
    }
    
    public BatchAndTimeQueue(CallBack callback, Integer size, Integer timeout) {
        this.callback = callback;
        this.size = size;
        this.timeout = timeout;
        pool = Executors.newFixedThreadPool(1);
    }

    public synchronized void put(E object) {
        if (!started.get()) {
            pool.execute(this);
        }
        
        queue.add(object);
        if (queue.size() >= size) {
            t.interrupt();
        }
    }
    
    public E poll() {
        return queue.poll();
    }
    
    @Override
    public void run() {
        started.set(true);
        try {
            Thread.sleep(timeout);
            LOGGER.debug("Timeout");
        } catch (InterruptedException e) {
            LOGGER.debug("Interupted");
        }
        
        callback.execute(this);
        started.set(false);
    }

    public int size() {
        return queue.size();
    }

    
}
