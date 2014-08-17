package com.ingby.socbox.bischeck.cache;

import java.util.LinkedList;

public class CacheQueue<E> extends LinkedList<E> {
    
    private static final long serialVersionUID = -1886983361399250646L;
    
    private Integer maxsize;

    public CacheQueue(Integer size) {
        super();
        maxsize = size; 
    }
    
    @Override
    public void addFirst(E ls) {
        if (maxsize == 0) {
            return;
        }
        
        if (size() >= maxsize) {
            removeLast();
        }
        
        super.addFirst(ls);
    }
    
    
}
