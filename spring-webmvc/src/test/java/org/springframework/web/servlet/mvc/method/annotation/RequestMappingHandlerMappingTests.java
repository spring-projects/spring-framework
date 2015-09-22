/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.junit.Assert.*;

/**
 * Tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class RequestMappingHandlerMappingTests {

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();

	private final RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
	{
		this.handlerMapping.setApplicationContext(wac);
	}


	@Test
	public void useRegisteredSuffixPatternMatch() {
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
	public void useRegisteredSuffixPatternMatchInitialization() {
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

		wac.registerSingleton("testController", ComposedAnnotationController.class);
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
		this.handlerMapping.setEmbeddedValueResolver(
				value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value
		);

		String[] patterns = new String[] { "/foo", "/${pattern}/bar" };
		String[] result = this.handlerMapping.resolveEmbeddedValuesInPatterns(patterns);

		assertArrayEquals(new String[] { "/foo", "/foo/bar" }, result);
	}

	@Test
	public void resolveRequestMappingViaComposedAnnotation() throws Exception {
		Class<?> clazz = ComposedAnnotationController.class;
		Method method = clazz.getMethod("handleInput");
		RequestMappingInfo info = this.handlerMapping.getMappingForMethod(method, clazz);

		assertNotNull(info);
		assertEquals(Collections.singleton("/input"), info.getPatternsCondition().getPatterns());
	}


	@Controller
	static class ComposedAnnotationController {

		@RequestMapping
		public void handle() {
		}

		@PostJson("/input")
		public void handleInput() {
		}
	}

	@RequestMapping(method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface PostJson {

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String[] value() default {};
	}

}
