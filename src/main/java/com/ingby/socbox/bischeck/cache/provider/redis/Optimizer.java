package com.ingby.socbox.bischeck.cache.provider.redis;

public class Optimizer {

	String key = null;
	int highindex = 0;
	
	public Optimizer(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
	
	public void setIndex(int index) {
		if (index > highindex)
			highindex = index;
	}
	
	public int getHighIndex() {
		return highindex;
	}
	
}
