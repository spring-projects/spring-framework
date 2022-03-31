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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.OverridingClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockConstruction;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Arjen Poutsma
 * @author luozhenyu
 */
class CachedIntrospectionResultsTests {

	@Test
	void acceptAndClearClassLoader() throws Exception {
		BeanWrapper bw = new BeanWrapperImpl(TestBean.class);
		assertThat(bw.isWritableProperty("name")).isTrue();
		assertThat(bw.isWritableProperty("age")).isTrue();
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(TestBean.class)).isTrue();

		ClassLoader child = new OverridingClassLoader(getClass().getClassLoader());
		Class<?> tbClass = child.loadClass("org.springframework.beans.testfixture.beans.TestBean");
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
	void clearClassLoaderForSystemClassLoader() throws Exception {
		BeanUtils.getPropertyDescriptors(ArrayList.class);
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(ArrayList.class)).isTrue();
		CachedIntrospectionResults.clearClassLoader(ArrayList.class.getClassLoader());
		assertThat(CachedIntrospectionResults.strongClassCache.containsKey(ArrayList.class)).isFalse();
	}

	@Test
	void shouldUseExtendedBeanInfoWhenApplicable() throws NoSuchMethodException, SecurityException {
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

	@Test
	void shouldBeInitializedOnceInParallel() throws Exception {
		final int nThreads = 10;
		ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		CountDownLatch countDownLatch = new CountDownLatch(nThreads);

		List<Future<List<CachedIntrospectionResults>>> futures = new ArrayList<>(nThreads);
		for (int i = 0; i < nThreads; i++) {
			futures.add(executor.submit(new ConstructionCallable(countDownLatch)));
		}

		List<CachedIntrospectionResults> resultsList = new ArrayList<>();
		for (Future<List<CachedIntrospectionResults>> future : futures) {
			resultsList.addAll(future.get());
		}
		executor.shutdown();

		assertThat(resultsList).hasSize(1);
	}

	private record ConstructionCallable(CountDownLatch countDownLatch)
			implements Callable<List<CachedIntrospectionResults>> {
		@Override
		public List<CachedIntrospectionResults> call() throws Exception {
			try (MockedConstruction<CachedIntrospectionResults> cachedIntrospectionResults =
					mockConstruction(CachedIntrospectionResults.class)) {
				countDownLatch.countDown();

				// Ensure executing at the same time
				countDownLatch.await();
				CachedIntrospectionResults.forClass(AbstractMap.SimpleImmutableEntry.class);

				// Return a copy of constructed objects to avoid being cleared
				return new ArrayList<>(cachedIntrospectionResults.constructed());
			}
		}
	}

}
