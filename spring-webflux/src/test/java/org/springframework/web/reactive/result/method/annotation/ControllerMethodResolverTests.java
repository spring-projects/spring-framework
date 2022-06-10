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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.method.ResolvableMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ControllerMethodResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ControllerMethodResolverTests {

	private ControllerMethodResolver methodResolver;

	private HandlerMethod handlerMethod;


	@BeforeEach
	public void setup() {
		ArgumentResolverConfigurer resolvers = new ArgumentResolverConfigurer();
		resolvers.addCustomResolver(new CustomArgumentResolver());
		resolvers.addCustomResolver(new CustomSyncArgumentResolver());

		ServerCodecConfigurer codecs = ServerCodecConfigurer.create();
		codecs.customCodecs().register(new ByteArrayDecoder());
		codecs.customCodecs().register(new ByteBufferDecoder());

		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.registerBean(TestControllerAdvice.class);
		applicationContext.refresh();

		this.methodResolver = new ControllerMethodResolver(
				resolvers, ReactiveAdapterRegistry.getSharedInstance(), applicationContext, codecs.getReaders());

		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::handle).method();
		this.handlerMethod = new HandlerMethod(new TestController(), method);
	}


	@Test
	public void requestMappingArgumentResolvers() {
		InvocableHandlerMethod invocable = this.methodResolver.getRequestMappingMethod(this.handlerMethod);
		List<HandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestBodyMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestPartMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelAttributeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CookieValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ExpressionValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(SessionAttributeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestAttributeMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(HttpEntityMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ErrorsMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ServerWebExchangeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PrincipalMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(SessionStatusMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(WebSessionMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ContinuationHandlerMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomSyncArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelAttributeMethodArgumentResolver.class);
	}

	@Test
	public void modelAttributeArgumentResolvers() {
		List<InvocableHandlerMethod> methods = this.methodResolver.getModelAttributeMethods(this.handlerMethod);

		assertThat(methods.size()).as("Expected one each from Controller + ControllerAdvice").isEqualTo(2);
		InvocableHandlerMethod invocable = methods.get(0);
		List<HandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelAttributeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CookieValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ExpressionValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(SessionAttributeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestAttributeMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ErrorsMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ServerWebExchangeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PrincipalMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(WebSessionMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ContinuationHandlerMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomSyncArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelAttributeMethodArgumentResolver.class);
	}

	@Test
	public void initBinderArgumentResolvers() {
		List<SyncInvocableHandlerMethod> methods =
				this.methodResolver.getInitBinderMethods(this.handlerMethod);

		assertThat(methods.size()).as("Expected one each from Controller + ControllerAdvice").isEqualTo(2);
		SyncInvocableHandlerMethod invocable = methods.get(0);
		List<SyncHandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CookieValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ExpressionValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestAttributeMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ServerWebExchangeMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomSyncArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
	}

	@Test
	public void exceptionHandlerArgumentResolvers() {
		InvocableHandlerMethod invocable = this.methodResolver.getExceptionHandlerMethod(
				new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason"), this.handlerMethod);

		assertThat(invocable).as("No match").isNotNull();
		assertThat(invocable.getBeanType()).isEqualTo(TestController.class);
		List<HandlerMethodArgumentResolver> resolvers = invocable.getResolvers();

		AtomicInteger index = new AtomicInteger(-1);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PathVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(MatrixVariableMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestHeaderMapMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CookieValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ExpressionValueMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(SessionAttributeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestAttributeMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(ModelMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ServerWebExchangeMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(PrincipalMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(WebSessionMethodArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(ContinuationHandlerMethodArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomArgumentResolver.class);
		assertThat(next(resolvers, index).getClass()).isEqualTo(CustomSyncArgumentResolver.class);

		assertThat(next(resolvers, index).getClass()).isEqualTo(RequestParamMethodArgumentResolver.class);
	}

	@Test
	public void exceptionHandlerFromControllerAdvice() {
		InvocableHandlerMethod invocable = this.methodResolver.getExceptionHandlerMethod(
				new IllegalStateException("reason"), this.handlerMethod);

		assertThat(invocable).isNotNull();
		assertThat(invocable.getBeanType()).isEqualTo(TestControllerAdvice.class);
	}


	private static HandlerMethodArgumentResolver next(
			List<? extends HandlerMethodArgumentResolver> resolvers, AtomicInteger index) {

		return resolvers.get(index.incrementAndGet());
	}


	@Controller
	static class TestController {

		@InitBinder
		void initDataBinder() {}

		@ModelAttribute
		void initModel() {}

		@GetMapping
		void handle() {}

		@ExceptionHandler
		void handleException(ResponseStatusException ex) {}

	}


	@ControllerAdvice
	static class TestControllerAdvice {

		@InitBinder
		void initDataBinder() {}

		@ModelAttribute
		void initModel() {}

		@ExceptionHandler
		void handleException(IllegalStateException ex) {}

	}


	static class CustomArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter p) {
			return false;
		}

		@Override
		public Mono<Object> resolveArgument(MethodParameter p, BindingContext c, ServerWebExchange e) {
			return null;
		}
	}


	static class CustomSyncArgumentResolver extends CustomArgumentResolver
			implements SyncHandlerMethodArgumentResolver {

		@Override
		public Object resolveArgumentValue(MethodParameter p, BindingContext c, ServerWebExchange e) {
			return null;
		}
	}

}
