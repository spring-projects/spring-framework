package org.springframework.cache.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;

public class FailSafeCache implements Cache {

	private static final Logger log = LoggerFactory.getLogger(FailSafeCache.class);
	
	private Cache underlyingCache;
	
	public FailSafeCache(Cache underlyingCache) {
		this.underlyingCache = underlyingCache;
	}

	public String getName() {
		return this.underlyingCache.getName();
	}

	public Object getNativeCache() {
		return this.underlyingCache.getNativeCache();
	}

	public ValueWrapper get(Object key) {
		ValueWrapper result = null;
		try {
			result = this.underlyingCache.get(key);
		} catch(Exception e) {
			log.error("Failed to get key: {}", key, e);
		}
		return result;
	}

	public void put(Object key, Object value) {
		try {
			this.underlyingCache.put(key, value);
		} catch(Exception e) {
			if(log.isErrorEnabled()) {
				log.error("Failed to put key/value: '{'" + key + ", {" + value + "} in the cache", e);
			}
		}
	}

	public void evict(Object key) {
		try {
			this.underlyingCache.evict(key);
		} catch(Exception e) {
			log.error("Failed to evict key: '{}'", key, e);
		}
	}

	public void clear() {
		try {
			this.underlyingCache.clear();
		} catch(Exception e) {
			log.error("Failed to clear cache", e);
		}
	}
}
