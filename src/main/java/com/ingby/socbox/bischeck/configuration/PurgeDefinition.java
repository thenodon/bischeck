package com.ingby.socbox.bischeck.configuration;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ingby.socbox.bischeck.cache.CacheUtil;

public class PurgeDefinition implements Comparable<PurgeDefinition> {
	private static final Logger LOGGER = LoggerFactory
            .getLogger(PurgeDefinition.class);

	private String purgeDefinition;
	private TYPE type;
	private String key;

	private boolean isTime;

	private Boolean isIndex;

	public enum TYPE {
		METRIC {
			public String toString() {
                return "metric";
            }
		},
		STATE {
			public String toString() {
                return "state";
            }
		},
		NOTIFICATION {
			public String toString() {
                return "notification";
            }
		};
	}
	
	/**
	 * 
	 * @param key
	 * @param type
	 * @param purgeDefinition
	 * @throws ConfigurationException
	 */
	public PurgeDefinition(String key, TYPE type, String purgeDefinition)
			throws ConfigurationException {
		this.key = key;
		this.type = type;
		this.purgeDefinition = purgeDefinition;
		
		if (TYPE.METRIC.equals(type)) {
			
		} else if (TYPE.STATE.equals(type) || TYPE.NOTIFICATION.equals(type)) {
			
		} else {
			LOGGER.error("Not a valid type {} for key {}",type, key);
			throw new ConfigurationException(String.format("Not a valid type %s for key %s",type, key));
		}
		
		isTime = CacheUtil.isByTime(purgeDefinition);
		
		try { 
			Long.valueOf(purgeDefinition);
			isIndex = true;
		} catch (NumberFormatException ne) {
			isIndex = false;
		}
	}

	public String getPurgeDefinition() {
		return purgeDefinition;
	}

	public TYPE getType() {
		return type;
	}

	public String getKey() {
		return key;
	}
	
	public Boolean isTime() {
		return isTime;
	}
	
	public Boolean isIndex() {
		return isIndex;
	}

	@Override
	public int compareTo(PurgeDefinition o) {
		 int keyCmp = key.compareTo(o.getKey());
		 return (keyCmp != 0 ? keyCmp : type.compareTo(o.getType())); 
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((isIndex == null) ? 0 : isIndex.hashCode());
		result = prime * result + (isTime ? 1231 : 1237);
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result
				+ ((purgeDefinition == null) ? 0 : purgeDefinition.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PurgeDefinition other = (PurgeDefinition) obj;
		if (isIndex == null) {
			if (other.isIndex != null)
				return false;
		} else if (!isIndex.equals(other.isIndex))
			return false;
		if (isTime != other.isTime)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (purgeDefinition == null) {
			if (other.purgeDefinition != null)
				return false;
		} else if (!purgeDefinition.equals(other.purgeDefinition))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
}
