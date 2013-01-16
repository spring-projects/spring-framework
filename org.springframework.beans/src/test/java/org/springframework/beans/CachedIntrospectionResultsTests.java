/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.util.ArrayList;

import org.junit.Test;
import test.beans.TestBean;

import org.springframework.core.OverridingClassLoader;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class CachedIntrospectionResultsTests {

	@Test
	public void acceptAndClearClassLoader() throws Exception {
		BeanWrapper bw = new BeanWrapperImpl(TestBean.class);
		assertTrue(bw.isWritableProperty("name"));
		assertTrue(bw.isWritableProperty("age"));
		assertTrue(CachedIntrospectionResults.classCache.containsKey(TestBean.class));

		ClassLoader child = new OverridingClassLoader(getClass().getClassLoader());
		Class<?> tbClass = child.loadClass("test.beans.TestBean");
		assertFalse(CachedIntrospectionResults.classCache.containsKey(tbClass));
		CachedIntrospectionResults.acceptClassLoader(child);
		bw = new BeanWrapperImpl(tbClass);
		assertTrue(bw.isWritableProperty("name"));
		assertTrue(bw.isWritableProperty("age"));
		assertTrue(CachedIntrospectionResults.classCache.containsKey(tbClass));
		CachedIntrospectionResults.clearClassLoader(child);
		assertFalse(CachedIntrospectionResults.classCache.containsKey(tbClass));

		assertTrue(CachedIntrospectionResults.classCache.containsKey(TestBean.class));
	}

	@Test
	public void clearClassLoaderForSystemClassLoader() throws Exception {
		BeanUtils.getPropertyDescriptors(ArrayList.class);
		assertTrue(CachedIntrospectionResults.classCache.containsKey(ArrayList.class));
		CachedIntrospectionResults.clearClassLoader(ArrayList.class.getClassLoader());
		assertFalse(CachedIntrospectionResults.classCache.containsKey(ArrayList.class));
	}

}
