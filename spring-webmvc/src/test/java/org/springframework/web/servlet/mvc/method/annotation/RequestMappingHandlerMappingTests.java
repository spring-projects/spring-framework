/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class RequestMappingHandlerMappingTests {

	@SuppressWarnings("unused")
	static Stream<Arguments> pathPatternsArguments() {
		RequestMappingHandlerMapping mapping1 = new RequestMappingHandlerMapping();
		StaticWebApplicationContext wac1 = new StaticWebApplicationContext();
		mapping1.setPatternParser(new PathPatternParser());
		mapping1.setApplicationContext(wac1);

		RequestMappingHandlerMapping mapping2 = new RequestMappingHandlerMapping();
		StaticWebApplicationContext wac2 = new StaticWebApplicationContext();
		mapping2.setApplicationContext(wac2);

		return Stream.of(Arguments.of(mapping1, wac1), Arguments.of(mapping2, wac2));
	}


	@Test
	@SuppressWarnings("deprecation")
	void useRegisteredSuffixPatternMatch() {

		RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
		handlerMapping.setApplicationContext(new StaticWebApplicationContext());

		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy = new org.springframework.web.accept.PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		handlerMapping.setContentNegotiationManager(manager);
		handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		handlerMapping.afterPropertiesSet();

		assertThat(handlerMapping.useSuffixPatternMatch()).isTrue();
		assertThat(handlerMapping.useRegisteredSuffixPatternMatch()).isTrue();
		assertThat(handlerMapping.getFileExtensions()).isEqualTo(Collections.singletonList("json"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void useRegisteredSuffixPatternMatchInitialization() {
		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy strategy = new org.springframework.web.accept.PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		final Set<String> extensions = new HashSet<>();

		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping() {
			@Override
			protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
				extensions.addAll(getFileExtensions());
				return super.getMappingForMethod(method, handlerType);
			}
		};

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("testController", ComposedAnnotationController.class);
		wac.refresh();

		mapping.setContentNegotiationManager(manager);
		mapping.setUseRegisteredSuffixPatternMatch(true);
		mapping.setApplicationContext(wac);
		mapping.afterPropertiesSet();

		assertThat(extensions).isEqualTo(Collections.singleton("json"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void suffixPatternMatchSettings() {
		RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();

		assertThat(handlerMapping.useSuffixPatternMatch()).isFalse();
		assertThat(handlerMapping.useRegisteredSuffixPatternMatch()).isFalse();

		handlerMapping.setUseRegisteredSuffixPatternMatch(false);
		assertThat(handlerMapping.useSuffixPatternMatch())
				.as("'false' registeredSuffixPatternMatch shouldn't impact suffixPatternMatch")
				.isFalse();

		handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		assertThat(handlerMapping.useSuffixPatternMatch())
				.as("'true' registeredSuffixPatternMatch should enable suffixPatternMatch")
				.isTrue();
	}

	@PathPatternsParameterizedTest
	void resolveEmbeddedValuesInPatterns(RequestMappingHandlerMapping mapping) {

		mapping.setEmbeddedValueResolver(
				value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value
		);

		String[] patterns = new String[] { "/foo", "/${pattern}/bar" };
		String[] result = mapping.resolveEmbeddedValuesInPatterns(patterns);

		assertThat(result).isEqualTo(new String[] { "/foo", "/foo/bar" });
	}

	@PathPatternsParameterizedTest
	void pathPrefix(RequestMappingHandlerMapping mapping) throws Exception {

		mapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
		mapping.setPathPrefixes(Collections.singletonMap(
				"/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));
		mapping.afterPropertiesSet();

		Method method = UserController.class.getMethod("getUser");
		RequestMappingInfo info = mapping.getMappingForMethod(method, UserController.class);

		assertThat(info).isNotNull();
		assertThat(info.getPatternValues()).isEqualTo(Collections.singleton("/api/user/{id}"));
	}

	@PathPatternsParameterizedTest // gh-23907
	void pathPrefixPreservesPathMatchingSettings(RequestMappingHandlerMapping mapping) throws Exception {
		mapping.setPathPrefixes(Collections.singletonMap("/api", HandlerTypePredicate.forAnyHandlerType()));
		mapping.afterPropertiesSet();

		Method method = ComposedAnnotationController.class.getMethod("get");
		RequestMappingInfo info = mapping.getMappingForMethod(method, ComposedAnnotationController.class);

		assertThat(info).isNotNull();
		assertThat(info.getActivePatternsCondition()).isNotNull();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/get");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNotNull();

		request = new MockHttpServletRequest("GET", "/api/get.pdf");
		initRequestPath(mapping, request);
		assertThat(info.getActivePatternsCondition().getMatchingCondition(request)).isNull();
	}

	private void initRequestPath(RequestMappingHandlerMapping mapping, MockHttpServletRequest request) {
		PathPatternParser parser = mapping.getPatternParser();
		if (parser != null) {
			ServletRequestPathUtils.parseAndCache(request);
		}
		else {
			mapping.getUrlPathHelper().resolveAndCacheLookupPath(request);
		}
	}

	@PathPatternsParameterizedTest
	void resolveRequestMappingViaComposedAnnotation(RequestMappingHandlerMapping mapping) {

		RequestMappingInfo info = assertComposedAnnotationMapping(
				mapping, "postJson", "/postJson", RequestMethod.POST);

		assertThat(info.getConsumesCondition().getConsumableMediaTypes().iterator().next().toString())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(info.getProducesCondition().getProducibleMediaTypes().iterator().next().toString())
				.isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test // SPR-14988
	void getMappingOverridesConsumesFromTypeLevelAnnotation() throws Exception {
		RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(RequestMethod.POST);

		ConsumesRequestCondition condition = requestMappingInfo.getConsumesCondition();
		assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));
	}

	@PathPatternsParameterizedTest // gh-22010
	void consumesWithOptionalRequestBody(RequestMappingHandlerMapping mapping, StaticWebApplicationContext wac) {
		wac.registerSingleton("testController", ComposedAnnotationController.class);
		wac.refresh();
		mapping.afterPropertiesSet();
		RequestMappingInfo result = mapping.getHandlerMethods().keySet().stream()
				.filter(info -> info.getPatternValues().equals(Collections.singleton("/post")))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No /post"));

		assertThat(result.getConsumesCondition().isBodyRequired()).isFalse();
	}

	@Test
	void getMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.GET);
	}

	@Test
	void postMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.POST);
	}

	@Test
	void putMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.PUT);
	}

	@Test
	void deleteMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.DELETE);
	}

	@Test
	void patchMapping() throws Exception {
		assertComposedAnnotationMapping(RequestMethod.PATCH);
	}

	private RequestMappingInfo assertComposedAnnotationMapping(RequestMethod requestMethod) throws Exception {

		RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
		mapping.setApplicationContext(new StaticWebApplicationContext());

		String methodName = requestMethod.name().toLowerCase();
		String path = "/" + methodName;

		return assertComposedAnnotationMapping(mapping, methodName, path, requestMethod);
	}

	private RequestMappingInfo assertComposedAnnotationMapping(
			RequestMappingHandlerMapping mapping, String methodName, String path, RequestMethod requestMethod) {

		Class<?> clazz = ComposedAnnotationController.class;
		Method method = ClassUtils.getMethod(clazz, methodName, (Class<?>[]) null);
		RequestMappingInfo info = mapping.getMappingForMethod(method, clazz);

		assertThat(info).isNotNull();

		Set<String> paths = info.getPatternValues();
		assertThat(paths.size()).isEqualTo(1);
		assertThat(paths.iterator().next()).isEqualTo(path);

		Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
		assertThat(methods.size()).isEqualTo(1);
		assertThat(methods.iterator().next()).isEqualTo(requestMethod);

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

		@GetMapping("/get")
		public void get() {
		}

		@PostMapping(path = "/post", consumes = MediaType.APPLICATION_XML_VALUE)
		public void post(@RequestBody(required = false) Foo foo) {
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

		@AliasFor(annotation = RequestMapping.class)
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


	private static class Foo {
	}

}
