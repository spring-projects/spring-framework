/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for PublishedEventsParameterResolver.
 *
 * @author Oliver Drotbohm
 */
class PublishedEventsExtensionUnitTests {

	AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test // #98
	void supportsPublishedEventsType() throws Exception {

		PublishedEventsExtension resolver = new PublishedEventsExtension(__ -> context);

		assertThat(resolver.supportsParameter(getParameterContext(PublishedEvents.class), null)).isTrue();
		assertThat(resolver.supportsParameter(getParameterContext(Object.class), null)).isFalse();
	}

	@Test // #98
	void createsThreadBoundPublishedEvents() throws Exception {

		PublishedEventsExtension resolver = new PublishedEventsExtension(__ -> context);
		context.refresh();

		resolver.beforeAll(null);

		Map<String, PublishedEvents> allEvents = new ConcurrentHashMap<>();
		List<String> keys = Arrays.asList("first", "second", "third");
		CountDownLatch latch = new CountDownLatch(3);

		for (String it : keys) {

			new Thread(() -> {

				PublishedEvents events = resolver.resolveParameter(null, null);
				context.publishEvent(it);
				allEvents.put(it, events);

				resolver.afterEach(null);

				latch.countDown();

			}).start();

		}

		latch.await(50, TimeUnit.MILLISECONDS);

		keys.forEach(it -> assertThat(allEvents.get(it).ofType(String.class)).containsExactly(it));
	}

	private static ParameterContext getParameterContext(Class<?> type) {

		Method method = ReflectionUtils.findMethod(Methods.class, "with", type);

		ParameterContext context = mock(ParameterContext.class);
		doReturn(method.getParameters()[0]).when(context).getParameter();

		return context;
	}

	interface Methods {

		void with(PublishedEvents events);

		void with(Object object);
	}
}
