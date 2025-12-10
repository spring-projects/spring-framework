/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link AbstractUrlHandlerMapping}.
 *
 * @author Stephane Nicoll
 */
class AbstractUrlHandlerMappingTests {

	private final AbstractUrlHandlerMapping mapping = new AbstractUrlHandlerMapping() {};

	@Test
	void registerRootHandler() {
		TestController rootHandler = new TestController();
		mapping.registerHandler("/", rootHandler);
		assertThat(mapping).satisfies(hasMappings(rootHandler, null, Map.of()));
	}

	@Test
	void registerWildcardHandler() throws Exception {
		TestController handler = new TestController();
		mapping.registerHandler("/*", handler);
		assertThat(mapping).satisfies(hasMappings(null, null, Map.of()));
		assertThat(getHandlerForPath("/abc")).isNotNull();
	}

	@Test
	void registerSpecificMapping() {
		TestController testHandler = new TestController();
		mapping.registerHandler("/test", testHandler);
		assertThat(mapping).satisfies(hasMappings(null, null, Map.of("/test", testHandler)));
	}

	@Test
	void registerSpecificMappingWithBeanName() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("controller", TestController.class);
		mapping.setApplicationContext(context);
		mapping.registerHandler("/test", "controller");
		assertThat(mapping.getHandlerMap().get("/test")).isSameAs(context.getBean("controller"));
	}

	@Test
	void unregisterRootHandler() {
		TestController rootHandler = new TestController();
		TestController specificHandler = new TestController();
		mapping.registerHandler("/", rootHandler);
		mapping.registerHandler("/test", specificHandler);
		assertThat(mapping).satisfies(hasMappings(rootHandler, null, Map.of("/test", specificHandler)));

		mapping.unregisterHandler("/");
		assertThat(mapping).satisfies(hasMappings(null, null, Map.of("/test", specificHandler)));
	}

	@Test
	void unregisterDefaultHandler() throws Exception {
		TestController wildcardHandler = new TestController();
		TestController specificHandler = new TestController();
		mapping.registerHandler("/*", wildcardHandler);
		mapping.registerHandler("/test", specificHandler);
		assertThat(mapping).satisfies(hasMappings(null, null, Map.of("/test", specificHandler)));
		assertThat(getHandlerForPath("/abc")).isNotNull();

		mapping.unregisterHandler("/*");
		assertThat(mapping).satisfies(hasMappings(null, null, Map.of("/test", specificHandler)));
		assertThat(getHandlerForPath("/abc")).isNull();
	}

	@Test
	void unregisterSpecificHandler() {
		TestController rootHandler = new TestController();
		TestController wildcardHandler = new TestController();
		TestController specificHandler = new TestController();
		mapping.registerHandler("/", rootHandler);
		mapping.registerHandler("/*", wildcardHandler);
		mapping.registerHandler("/test", specificHandler);
		assertThat(mapping).satisfies(hasMappings(rootHandler, null, Map.of("/test", specificHandler)));

		mapping.unregisterHandler("/test");
		assertThat(mapping).satisfies(hasMappings(rootHandler, null, Map.of()));
	}

	@Test
	void unregisterUnsetRootHandler() {
		assertThatNoException().isThrownBy(() -> mapping.unregisterHandler("/"));
	}

	@Test
	void unregisterUnsetDefaultHandler() {
		assertThatNoException().isThrownBy(() -> mapping.unregisterHandler("/*"));
	}

	@Test
	void unregisterUnknownHandler() {
		TestController specificHandler = new TestController();
		mapping.registerHandler("/test", specificHandler);

		mapping.unregisterHandler("/test/*");
		assertThat(mapping.getHandlerMap()).containsExactly(entry("/test", specificHandler));
	}


	private Consumer<AbstractUrlHandlerMapping> hasMappings(
			@Nullable Object rootHandler, @Nullable Object defaultHandler, Map<String, Object> handlerMap) {

		return actual -> {
			assertThat(actual.getRootHandler()).isEqualTo(rootHandler);
			assertThat(actual.getDefaultHandler()).isEqualTo(defaultHandler);
			assertThat(actual.getHandlerMap()).containsExactlyEntriesOf(handlerMap);
		};
	}

	private Object getHandlerForPath(String path) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		return mapping.getHandlerInternal(request);
	}


	private static class TestController {}

}
