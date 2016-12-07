/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.reactive.accept.MappingContentTypeResolver;
import org.springframework.web.reactive.result.method.RequestMappingInfo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerMappingTests {

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();

	private final RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();


	@Before
	public void setUp() throws Exception {
		this.handlerMapping.setApplicationContext(wac);
	}


	@Test
	public void useRegisteredSuffixPatternMatch() {
		assertTrue(this.handlerMapping.useSuffixPatternMatch());
		assertTrue(this.handlerMapping.useRegisteredSuffixPatternMatch());

		MappingContentTypeResolver contentTypeResolver = mock(MappingContentTypeResolver.class);
		when(contentTypeResolver.getKeys()).thenReturn(Collections.singleton("json"));

		this.handlerMapping.setContentTypeResolver(contentTypeResolver);
		this.handlerMapping.afterPropertiesSet();

		assertTrue(this.handlerMapping.useSuffixPatternMatch());
		assertTrue(this.handlerMapping.useRegisteredSuffixPatternMatch());
		assertEquals(Collections.singleton("json"), this.handlerMapping.getFileExtensions());
	}

	@Test
	public void useRegisteredSuffixPatternMatchInitialization() {
		MappingContentTypeResolver contentTypeResolver = mock(MappingContentTypeResolver.class);
		when(contentTypeResolver.getKeys()).thenReturn(Collections.singleton("json"));

		final Set<String> actualExtensions = new HashSet<>();
		RequestMappingHandlerMapping localHandlerMapping = new RequestMappingHandlerMapping() {
			@Override
			protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
				actualExtensions.addAll(getFileExtensions());
				return super.getMappingForMethod(method, handlerType);
			}
		};
		this.wac.registerSingleton("testController", ComposedAnnotationController.class);
		this.wac.refresh();

		localHandlerMapping.setContentTypeResolver(contentTypeResolver);
		localHandlerMapping.setUseRegisteredSuffixPatternMatch(true);
		localHandlerMapping.setApplicationContext(this.wac);
		localHandlerMapping.afterPropertiesSet();

		assertEquals(Collections.singleton("json"), actualExtensions);
	}

	@Test
	public void useSuffixPatternMatch() {
		assertTrue(this.handlerMapping.useSuffixPatternMatch());
		assertTrue(this.handlerMapping.useRegisteredSuffixPatternMatch());

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
		RequestMappingInfo info = assertComposedAnnotationMapping("postJson", "/postJson", RequestMethod.POST);

		assertEquals(MediaType.APPLICATION_JSON_VALUE,
			info.getConsumesCondition().getConsumableMediaTypes().iterator().next().toString());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
			info.getProducesCondition().getProducibleMediaTypes().iterator().next().toString());
	}

	/**
	 * SPR-14988: Add consumes() attribute to @GetMapping annotation
	 */
	@Test
	public void getMappingOverridesConsumesFromTypeLevelAnnotation() throws Exception {
		RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(RequestMethod.GET);

		assertArrayEquals(new MediaType[]{MediaType.ALL},
				new ArrayList<MediaType>(requestMappingInfo.getConsumesCondition().getConsumableMediaTypes()).toArray());
	}

	@Test
	public void getMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.GET);
	}

	@Test
	public void postMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.POST);
	}

	@Test
	public void putMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.PUT);
	}

	@Test
	public void deleteMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.DELETE);
	}

	@Test
	public void patchMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.PATCH);
	}

	private RequestMappingInfo assertComposedAnnotationMapping(RequestMethod requestMethod) throws Exception {
		String methodName = requestMethod.name().toLowerCase();
		String path = "/" + methodName;

		return assertComposedAnnotationMapping(methodName, path, requestMethod);
	}

	private RequestMappingInfo assertComposedAnnotationMapping(String methodName, String path,
			RequestMethod requestMethod) throws Exception {

		Class<?> clazz = ComposedAnnotationController.class;
		Method method = clazz.getMethod(methodName);
		RequestMappingInfo info = this.handlerMapping.getMappingForMethod(method, clazz);

		assertNotNull(info);

		Set<String> paths = info.getPatternsCondition().getPatterns();
		assertEquals(1, paths.size());
		assertEquals(path, paths.iterator().next());

		Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
		assertEquals(1, methods.size());
		assertEquals(requestMethod, methods.iterator().next());

		return info;
	}


	@Controller @SuppressWarnings("unused")
	@RequestMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	static class ComposedAnnotationController {

		@RequestMapping
		public void handle() {
		}

		@PostJson("/postJson")
		public void postJson() {
		}

		@GetMapping(value = "/get", consumes = MediaType.ALL_VALUE)
		public void get() {
		}

		@PostMapping("/post")
		public void post() {
		}

		@PutMapping("/put")
		public void put() {
		}

		@DeleteMapping("/delete")
		public void delete() {
		}

		@PatchMapping("/patch")
		public void patch() {
		}

	}

	@RequestMapping(method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface PostJson {

		@AliasFor(annotation = RequestMapping.class, attribute = "path") @SuppressWarnings("unused")
		String[] value() default {};
	}

}
