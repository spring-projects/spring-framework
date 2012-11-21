package org.springframework.cache.support;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

public class AbstractCacheManagerTest {

	private AbstractCacheManager uut;
	
	private static final String CACHE_NAME = "test";

	private class AbstractCacheManagerImpl extends AbstractCacheManager {

		@Override
		protected Collection<? extends Cache> loadCaches() {
			Collection<Cache> caches = new LinkedHashSet<Cache>();
			caches.add(new ConcurrentMapCache(CACHE_NAME));
			return caches;
		}
	}
	
	@Before
	public void setup() {
		uut = new AbstractCacheManagerImpl();
	}
	
	@Test
	public void testName() throws Exception {
		uut.afterPropertiesSet();
		
		Cache cache = uut.getCache(CACHE_NAME);
		
		assertTrue(cache instanceof ConcurrentMapCache);
		assertFalse(cache instanceof FailSafeCache);
	}
	
	@Test
	public void testName2() {
		uut.setGracefullyHandleExceptions(true);
		uut.afterPropertiesSet();
		
		Cache cache = uut.getCache(CACHE_NAME);
		
		assertTrue(cache instanceof FailSafeCache);
	}
}
