/*
#
# Copyright (C) 2010-2015 Anders Håål, Ingenjorsbyn AB
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
 */

package com.ingby.socbox.bischeck.servers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BatchAndTimeQueue manage a queue that is populated by on the thread and
 * emptied by a separated thread managed by the class executing the
 * {@link CallBack#batchListener(ServerQueue)} for the registered callback.
 * 
 * @param <E>
 */
public class ServerQueue<E> implements Runnable {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ServerQueue.class);

    private static final int WAIT_TERMINIATION_MS = 10000;

    @SuppressWarnings("rawtypes")
    private BatchListenerInf callback;
    private Integer thresholdSize;
    private Integer threshholdTimeout;
    private BlockingQueue<E> queue = new LinkedBlockingQueue<>();

    // Thread t = new Thread("Timer");
    private ExecutorService pool;
    private AtomicBoolean started = new AtomicBoolean(false);

    private boolean batchMode;

    private Object wakeUp = new Object();

    private int maxSize;

    public static void main(String[] args) {

        ServerQueue<String> b = new ServerQueue<>(
                new BatchListenerInf<List<String>>() {
                    @Override
                    public void batchListener(List<String> list) {
                        LOGGER.debug("Doing remote call!!!!!!!!!!!!!!!!!!!!");
                        for (String str : list) {
                            LOGGER.debug(str);
                        }
                        LOGGER.debug("Done remote call!!!!!!!!!!!!!!!!!!!!");
                    }
                }, 10, 3000);

        int count = 0;
        while (count < 100) {
            LOGGER.debug("Hello" + count++);
            b.put("Hello" + count);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
        }

        LOGGER.debug("NO BATCH");
        b = new ServerQueue<>();

        count = 0;
        while (count < 100) {
            LOGGER.debug("Hello" + count++);
            b.put("Hello" + count);
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
        }
    }

    /**
     * Constructor
     * 
     * @param callback
     *            the class to be called c
     * @param size
     *            the size of the queue before the callback is called
     * @param timeout
     *            the time in milliseconds before the callback is called
     */
    @SuppressWarnings("rawtypes")
    public ServerQueue(BatchListenerInf callback, Integer size, Integer timeout) {
        this.callback = callback;
        this.thresholdSize = size;
        this.threshholdTimeout = timeout;
        this.batchMode = true;
        this.maxSize = 10;
        pool = Executors.newCachedThreadPool();
    }

    @SuppressWarnings("rawtypes")
    public ServerQueue(BatchListenerInf callback, Integer size,
            Integer timeout, Integer maxChunck) {
        this.callback = callback;
        this.thresholdSize = size;
        this.threshholdTimeout = timeout;
        this.batchMode = true;
        this.maxSize = maxChunck;
        pool = Executors.newCachedThreadPool();
    }

    public ServerQueue() {
        this.batchMode = false;
    }

    /**
     * Put a element into the end of the queue
     * 
     * @param object
     */
    public void put(E object) {

        if (batchMode && !started.get()) {
            pool.execute(this);
        }

        queue.add(object);
        if (batchMode && queue.size() >= thresholdSize) {
            if (queue.size() > thresholdSize * 2) {
                LOGGER.debug("ADD THREAD IN POOL");
                pool.execute(this);
            }
            synchronized (wakeUp) {
                LOGGER.debug("Interupted by size");
                wakeUp.notifyAll();
            }
        }
    }

    /**
     * Return the head element of the queue
     * 
     * @return
     */
    public E poll() {
        return queue.poll();
    }

    public E take() throws InterruptedException {
        return queue.take();
    }

    public int size() {
        return queue.size();
    }

    /**
     * Return all available elements in the queue
     * 
     * @return list of all elements
     */
    private List<E> pollAll(int maxSize) {
        List<E> list = new ArrayList<>();
        int count = queue.size();
        LOGGER.debug("Size is {}", count);
        synchronized (queue) {
            for (int i = 0; i < maxSize; i++) {
                E element = queue.poll();
                if (element != null) {
                    list.add(element);
                }
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        started.set(true);
        synchronized (wakeUp) {
            try {
                wakeUp.wait(threshholdTimeout);
                LOGGER.debug("Timeout or size");
            } catch (InterruptedException e) {
                LOGGER.debug("Interupted");
            }
        }
        callback.batchListener(pollAll(maxSize));
        started.set(false);
    }

    public void destory() {
        LOGGER.info("Unregister called");

        if (pool != null) {
            pool.shutdown();

            pool.shutdownNow();

            try {
                pool.awaitTermination(WAIT_TERMINIATION_MS / 2,
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException e1) {
            }

            LOGGER.info("Shutdown is done");

            for (int waitCount = 0; waitCount < 3; waitCount++) {
                try {
                    if (pool.awaitTermination(WAIT_TERMINIATION_MS,
                            TimeUnit.MILLISECONDS) && pool.isTerminated()) {
                        LOGGER.info("ExecutorService and all workers terminated");
                        break;
                    }
                } catch (InterruptedException e) {
                }
            }
            LOGGER.info("All workers stopped");
        }
    }
}
