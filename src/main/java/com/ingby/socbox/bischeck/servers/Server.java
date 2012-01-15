package com.ingby.socbox.bischeck.servers;

import com.ingby.socbox.bischeck.service.Service;

public interface Server {
    
    /**
     * Send the Service information to the server. Implementation is responsible
     * to manage protocol and formatting of message data.
     * @param service
     */
    public void send(Service service);

    

}
