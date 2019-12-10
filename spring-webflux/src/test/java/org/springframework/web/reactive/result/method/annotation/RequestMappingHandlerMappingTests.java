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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
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
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RequestMappingHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerMappingTests {

	private final StaticWebApplicationContext wac = new StaticWebApplicationContext();

	private final RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();


	@BeforeEach
	public void setup() {
		this.handlerMapping.setApplicationContext(wac);
	}


	@Test
	public void resolveEmbeddedValuesInPatterns() {
		this.handlerMapping.setEmbeddedValueResolver(value -> "/${pattern}/bar".equals(value) ? "/foo/bar" : value);

		String[] patterns = new String[] { "/foo", "/${pattern}/bar" };
		String[] result = this.handlerMapping.resolveEmbeddedValuesInPatterns(patterns);

		assertThat(result).isEqualTo(new String[] { "/foo", "/foo/bar" });
	}

	@Test
	public void pathPrefix() throws Exception {
		this.handlerMapping.setEmbeddedValueResolver(value -> "/${prefix}".equals(value) ? "/api" : value);
		this.handlerMapping.setPathPrefixes(Collections.singletonMap(
				"/${prefix}", HandlerTypePredicate.forAnnotation(RestController.class)));

		Method method = UserController.class.getMethod("getUser");
		RequestMappingInfo info = this.handlerMapping.getMappingForMethod(method, UserController.class);

		assertThat(info).isNotNull();
		assertThat(info.getPatternsCondition().getPatterns()).isEqualTo(Collections.singleton(new PathPatternParser().parse("/api/user/{id}")));
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
				.filter(i -> {
					PatternsRequestCondition condition = i.getPatternsCondition();
					return condition.getPatterns().iterator().next().getPatternString().equals("/post");
				})
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

		Set<PathPattern> paths = info.getPatternsCondition().getPatterns();
		assertThat(paths.size()).isEqualTo(1);
		assertThat(paths.iterator().next().getPatternString()).isEqualTo(path);

		Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
		assertThat(methods.size()).isEqualTo(1);
		assertThat(methods.iterator().next()).isEqualTo(requestMethod);

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

	private static class Foo {
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


	@RestController
	@RequestMapping("/user")
	static class UserController {

		@GetMapping("/{id}")
		public Principal getUser() {
			return mock(Principal.class);
		}
	}

}
