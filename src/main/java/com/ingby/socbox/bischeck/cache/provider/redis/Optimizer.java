package com.ingby.socbox.bischeck.cache.provider.redis;

public class Optimizer {

	String key = null;
	long highindex = 0;
	
	public Optimizer(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
	
	public void setIndex(long index) {
		if (index > highindex) {
			highindex = index;
		}
	}
	
	public long getHighIndex() {
		return highindex;
	}
	
}
