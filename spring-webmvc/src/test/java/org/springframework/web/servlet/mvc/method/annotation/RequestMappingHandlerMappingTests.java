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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

		final Set<String> extensions = new HashSet<>();

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
	public void pathPrefix() throws NoSuchMethodException {
		this.handlerMapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
		this.handlerMapping.setPathPrefixes(Collections.singletonMap(
				"/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));

		Method method = UserController.class.getMethod("getUser");
		RequestMappingInfo info = this.handlerMapping.getMappingForMethod(method, UserController.class);

		assertNotNull(info);
		assertEquals(Collections.singleton("/api/user/{id}"), info.getPatternsCondition().getPatterns());
	}

	@Test
	public void resolveRequestMappingViaComposedAnnotation() throws Exception {
		RequestMappingInfo info = assertComposedAnnotationMapping("postJson", "/postJson", RequestMethod.POST);

		assertEquals(MediaType.APPLICATION_JSON_VALUE,
			info.getConsumesCondition().getConsumableMediaTypes().iterator().next().toString());
		assertEquals(MediaType.APPLICATION_JSON_VALUE,
			info.getProducesCondition().getProducibleMediaTypes().iterator().next().toString());
	}

	@Test // SPR-14988
	public void getMappingOverridesConsumesFromTypeLevelAnnotation() throws Exception {
		RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(RequestMethod.GET);

		assertArrayEquals(new MediaType[]{MediaType.ALL}, new ArrayList<>(
				requestMappingInfo.getConsumesCondition().getConsumableMediaTypes()).toArray());
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


	@Controller
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

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String[] value() default {};
	}


	@RestController
	@RequestMapping("/user")
	static class UserController {

		@GetMapping("/{id}")
		public Principal getUser() {
			return mock(Principal.class);
		}
	}

}
