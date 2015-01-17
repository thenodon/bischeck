package com.ingby.socbox.bischeck.servers.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class CallBackTestImpl implements CallBack {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CallBackTestImpl.class);

    @Override
    public void execute(BatchAndTimeQueue queue) {
        int count = queue.size();
        LOGGER.debug("Size is {}", count);
        for (int i=0; i<count; i++) {
            LOGGER.debug("Empty {}", queue.poll());
        }

    }

}
