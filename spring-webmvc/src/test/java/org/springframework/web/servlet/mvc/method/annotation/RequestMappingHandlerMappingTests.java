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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
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
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
		assertThat(this.handlerMapping.useSuffixPatternMatch()).isTrue();
		assertThat(this.handlerMapping.useRegisteredSuffixPatternMatch()).isFalse();

		Map<String, MediaType> fileExtensions = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy(fileExtensions);
		ContentNegotiationManager manager = new ContentNegotiationManager(strategy);

		this.handlerMapping.setContentNegotiationManager(manager);
		this.handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		this.handlerMapping.afterPropertiesSet();

		assertThat(this.handlerMapping.useSuffixPatternMatch()).isTrue();
		assertThat(this.handlerMapping.useRegisteredSuffixPatternMatch()).isTrue();
		assertThat(this.handlerMapping.getFileExtensions()).isEqualTo(Arrays.asList("json"));
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

		assertThat(extensions).isEqualTo(Collections.singleton("json"));
	}

	@Test
	public void useSuffixPatternMatch() {
		assertThat(this.handlerMapping.useSuffixPatternMatch()).isTrue();

		this.handlerMapping.setUseSuffixPatternMatch(false);
		assertThat(this.handlerMapping.useSuffixPatternMatch()).isFalse();

		this.handlerMapping.setUseRegisteredSuffixPatternMatch(false);
		assertThat(this.handlerMapping.useSuffixPatternMatch()).as("'false' registeredSuffixPatternMatch shouldn't impact suffixPatternMatch").isFalse();

		this.handlerMapping.setUseRegisteredSuffixPatternMatch(true);
		assertThat(this.handlerMapping.useSuffixPatternMatch()).as("'true' registeredSuffixPatternMatch should enable suffixPatternMatch").isTrue();
	}

	@Test
	public void resolveEmbeddedValuesInPatterns() {
		this.handlerMapping.setEmbeddedValueResolver(
				value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value
		);

		String[] patterns = new String[] { "/foo", "/${pattern}/bar" };
		String[] result = this.handlerMapping.resolveEmbeddedValuesInPatterns(patterns);

		assertThat(result).isEqualTo(new String[] { "/foo", "/foo/bar" });
	}

	@Test
	public void pathPrefix() throws NoSuchMethodException {
		this.handlerMapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
		this.handlerMapping.setPathPrefixes(Collections.singletonMap(
				"/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));

		Method method = UserController.class.getMethod("getUser");
		RequestMappingInfo info = this.handlerMapping.getMappingForMethod(method, UserController.class);

		assertThat(info).isNotNull();
		assertThat(info.getPatternsCondition().getPatterns()).isEqualTo(Collections.singleton("/api/user/{id}"));
	}

	@Test
	public void resolveRequestMappingViaComposedAnnotation() throws Exception {
		RequestMappingInfo info = assertComposedAnnotationMapping("postJson", "/postJson", RequestMethod.POST);

		assertThat(info.getConsumesCondition().getConsumableMediaTypes().iterator().next().toString()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(info.getProducesCondition().getProducibleMediaTypes().iterator().next().toString()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test // SPR-14988
	public void getMappingOverridesConsumesFromTypeLevelAnnotation() throws Exception {
		RequestMappingInfo requestMappingInfo = assertComposedAnnotationMapping(RequestMethod.POST);

		ConsumesRequestCondition condition = requestMappingInfo.getConsumesCondition();
		assertThat(condition.getConsumableMediaTypes()).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));
	}

	@Test // gh-22010
	public void consumesWithOptionalRequestBody() {
		this.wac.registerSingleton("testController", ComposedAnnotationController.class);
		this.wac.refresh();
		this.handlerMapping.afterPropertiesSet();
		RequestMappingInfo info = this.handlerMapping.getHandlerMethods().keySet().stream()
				.filter(i -> i.getPatternsCondition().getPatterns().equals(Collections.singleton("/post")))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No /post"));

		assertThat(info.getConsumesCondition().isBodyRequired()).isFalse();
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
		Method method = ClassUtils.getMethod(clazz, methodName, (Class<?>[]) null);
		RequestMappingInfo info = this.handlerMapping.getMappingForMethod(method, clazz);

		assertThat(info).isNotNull();

		Set<String> paths = info.getPatternsCondition().getPatterns();
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
