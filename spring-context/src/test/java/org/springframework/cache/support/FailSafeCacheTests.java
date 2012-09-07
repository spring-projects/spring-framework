package org.springframework.cache.support;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.support.FailSafeCache;
import org.springframework.cache.support.SimpleValueWrapper;

public class FailSafeCacheTests {

	private Cache uut;
	
	private Cache mockUnderlyingCache;
	
	private Object mockKey;
	
	private Object mockValue;

	@Before
	public void setup() {
		mockUnderlyingCache = createMock(Cache.class);
		uut = new FailSafeCache(mockUnderlyingCache);
		
		mockKey = new Object();
		mockValue = new Object();
	}

	@After
	public void tearDown() {
		verify(mockUnderlyingCache);
	}
	
	@Test
	public void clearShouldDelegateToUnderlyingCache() {
		mockUnderlyingCache.clear();
		replay(mockUnderlyingCache);
		
		uut.clear();
	}
	
	@Test
	public void clearShouldFailGracefullyWhenUnderlyingCacheThrowsException() {
		mockUnderlyingCache.clear();
		expectLastCall().andThrow(new RuntimeException("Simulated Exception"));
		replay(mockUnderlyingCache);
		
		uut.clear();
	}
	
	@Test
	public void evictShouldDelegateToUnderlyingCache() {
		mockUnderlyingCache.evict(mockKey);
		replay(mockUnderlyingCache);
		
		uut.evict(mockKey);
	}
	
	@Test
	public void evictShouldFailGracefullyWhenUnderlyingCacheThrowsException() {
		mockUnderlyingCache.evict(mockKey);
		expectLastCall().andThrow(new RuntimeException("Simulated Exception"));
		replay(mockUnderlyingCache);
		
		uut.evict(mockKey);
	}
	
	@Test
	public void putShouldDelegateToUnderlyingCache() {
		mockUnderlyingCache.put(mockKey, mockValue);
		replay(mockUnderlyingCache);
		
		uut.put(mockKey, mockValue);
	}
	
	@Test
	public void putShouldFailGracefullyWhenUnderlyingCacheThrowsException() {
		mockUnderlyingCache.put(mockKey, mockValue);
		expectLastCall().andThrow(new RuntimeException("Simulated Exception"));
		replay(mockUnderlyingCache);
		
		uut.put(mockKey, mockValue);
	}
	
	@Test
	public void getShouldDelegateToUnderlyingCache() {
		ValueWrapper expectedValueWrapper = new SimpleValueWrapper(null);
		expect(mockUnderlyingCache.get(mockKey)).andReturn(expectedValueWrapper);
		replay(mockUnderlyingCache);
		
		ValueWrapper valueWrapper = uut.get(mockKey);
		
		assertEquals(expectedValueWrapper, valueWrapper);
	}
	
	@Test
	public void getShouldReturnNullWhenUnderlyingCacheThrowsException() {
		expect(mockUnderlyingCache.get(mockKey)).andThrow(new RuntimeException("Simulated Exception"));
		replay(mockUnderlyingCache);

		ValueWrapper valueWrapper = uut.get(mockKey);
		
		assertNull(valueWrapper);
	}
	
	@Test
	public void getNativeCacheShouldDelegateToUnderlyingCache() {
		Object expectedNativeCache = new Object();
		expect(mockUnderlyingCache.getNativeCache()).andReturn(expectedNativeCache);
		replay(mockUnderlyingCache);
		
		Object nativeCache = uut.getNativeCache();
		
		assertEquals(expectedNativeCache, nativeCache);
	}
	
	@Test
	public void getNameShouldDelegateToUnderlyingCache() {
		String expectedName = "Expected Name";
		expect(mockUnderlyingCache.getName()).andReturn(expectedName);
		replay(mockUnderlyingCache);
		
		String name = uut.getName();
		assertEquals(expectedName, name);
	}
}
