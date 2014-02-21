package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.GeneratedCacheKey;

import org.springframework.cache.interceptor.SimpleKey;

/**
 * A {@link SimpleKey} that implements the {@link GeneratedCacheKey} contract
 * required by JSR-107
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public final class SimpleGeneratedCacheKey extends SimpleKey implements GeneratedCacheKey {

	public SimpleGeneratedCacheKey(Object... elements) {
		super(elements);
	}

}
