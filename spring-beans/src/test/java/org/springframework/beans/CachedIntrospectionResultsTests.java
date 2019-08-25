/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import org.springframework.core.OverridingClassLoader;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Arjen Poutsma
 */
public class CachedIntrospectionResultsTests {

	@Test
	public void acceptAndClearClassLoader() throws Exception {
		BeanWrapper bw = new BeanWrapperImpl(TestBean.class);
		assertThat(bw.isWritableProperty("name")).isTrue();
		assertThat(bw.isWritableProperty("age")).isTrue();
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(TestBean.class)).isTrue();

		ClassLoader child = new OverridingClassLoader(getClass().getClassLoader());
		Class<?> tbClass = child.loadClass("org.springframework.tests.sample.beans.TestBean");
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(tbClass)).isFalse();
		CachedIntrospectionResults.acceptClassLoader(child);
		bw = new BeanWrapperImpl(tbClass);
		assertThat(bw.isWritableProperty("name")).isTrue();
		assertThat(bw.isWritableProperty("age")).isTrue();
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(tbClass)).isTrue();
		CachedIntrospectionResults.clearClassLoader(child);
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(tbClass)).isFalse();

		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(TestBean.class)).isTrue();
	}

	@Test
	public void clearClassLoaderForSystemClassLoader() throws Exception {
		BeanUtils.getPropertyDescriptors(ArrayList.class);
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(ArrayList.class)).isTrue();
		CachedIntrospectionResults.clearClassLoader(ArrayList.class.getClassLoader());
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(ArrayList.class)).isFalse();
	}

	@Test
	public void shouldUseExtendedBeanInfoWhenApplicable() throws NoSuchMethodException, SecurityException {
		// given a class with a non-void returning setter method
		@SuppressWarnings("unused")
		class C {
			public Object setFoo(String s) { return this; }
			public String getFoo() { return null; }
		}

		// CachedIntrospectionResults should delegate to ExtendedBeanInfo
		CachedIntrospectionResults results = CachedIntrospectionResults.forClass(C.class);
		BeanInfo info = results.getBeanInfo();
		PropertyDescriptor pd = null;
		for (PropertyDescriptor candidate : info.getPropertyDescriptors()) {
			if (candidate.getName().equals("foo")) {
				pd = candidate;
			}
		}

		// resulting in a property descriptor including the non-standard setFoo method
		assertThat(pd).isNotNull();
		assertThat(pd.getReadMethod()).isEqualTo(C.class.getMethod("getFoo"));
		// No write method found for non-void returning 'setFoo' method.
		// Check to see if CachedIntrospectionResults is delegating to ExtendedBeanInfo as expected
		assertThat(pd.getWriteMethod()).isEqualTo(C.class.getMethod("setFoo", String.class));
	}

}
