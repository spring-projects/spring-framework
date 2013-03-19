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
package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/**
 * Tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerMappingTests {

	private RequestMappingHandlerMapping handlerMapping;

	@Before
	public void setup() {
		this.handlerMapping = new RequestMappingHandlerMapping();
		this.handlerMapping.setApplicationContext(new StaticWebApplicationContext());
	}

	@Test
	public void useRegsiteredSuffixPatternMatch() {
		assertTrue(this.handlerMapping.useSuffixPatternMatch());
		assertFalse(this.handlerMapping.useRegisteredSuffixPatternMatch());

		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		this.handlerMapping.setContentNegotiationManager(manager);
		this.handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		this.handlerMapping.afterPropertiesSet();

		assertTrue(this.handlerMapping.useSuffixPatternMatch());
		assertTrue(this.handlerMapping.useRegisteredSuffixPatternMatch());
		assertEquals(Arrays.asList("json"), this.handlerMapping.getFileExtensions());
	}

	@Test
	public void useRegsiteredSuffixPatternMatchInitialization() {

		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		final Set<String> extensions = new HashSet<String>();

		RequestMappingHandlerMapping hm = new RequestMappingHandlerMapping() {
			@Override
			protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
				extensions.addAll(getFileExtensions());
				return super.getMappingForMethod(method, handlerType);
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("testController", TestController.class);
		wac.refresh();

		hm.setContentNegotiationManager(manager);
		hm.setUseRegisteredSuffixPatternMatch(true);
		hm.setApplicationContext(wac);
		hm.afterPropertiesSet();

		assertEquals(Collections.singleton("json"), extensions);
	}

	@Test
	public void useSuffixPatternMatch() {
		assertTrue(this.handlerMapping.useSuffixPatternMatch());

		this.handlerMapping.setUseSuffixPatternMatch(false);
		assertFalse(this.handlerMapping.useSuffixPatternMatch());

		this.handlerMapping.setUseRegisteredSuffixPatternMatch(false);
		assertFalse("'false' registeredSuffixPatternMatch shouldn't impact suffixPatternMatch",
				this.handlerMapping.useSuffixPatternMatch());

		this.handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		assertTrue("'true' registeredSuffixPatternMatch should enable suffixPatternMatch",
				this.handlerMapping.useSuffixPatternMatch());
	}

	@Test
	public void resolveEmbeddedValuesInPatterns() {
		this.handlerMapping.setEmbeddedValueResolver(new StringValueResolver() {
			public String resolveStringValue(String value) {
				return "/${pattern}/bar".equals(value) ? "/foo/bar" : value;
			}
		});

		String[] patterns = new String[] { "/foo", "/${pattern}/bar" };
		String[] result = this.handlerMapping.resolveEmbeddedValuesInPatterns(patterns);

		assertArrayEquals(new String[] { "/foo", "/foo/bar" }, result);
	}


	@Controller
	static class TestController {

		@RequestMapping
		public void handle() {
		}
	}

}
