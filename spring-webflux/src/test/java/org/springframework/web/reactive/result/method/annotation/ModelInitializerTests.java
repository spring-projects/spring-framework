/*
 * Copyright 2002-2017 the original author or authors.
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

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import reactor.core.publisher.Mono;
import rx.Single;

import org.springframework.core.MethodIntrospector;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.ui.Model;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter.ATTRIBUTE_METHODS;
import static org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter.BINDER_METHODS;

/**
 * Unit tests for {@link ModelInitializer}.
 * @author Rossen Stoyanchev
 */
public class ModelInitializerTests {

	private final ModelInitializer modelInitializer = new ModelInitializer(new ReactiveAdapterRegistry());

	private final ServerWebExchange exchange = MockServerHttpRequest.get("/path").toExchange();


	@SuppressWarnings("unchecked")
	@Test
	public void basic() throws Exception {
		TestController controller = new TestController();

		Validator validator = mock(Validator.class);
		controller.setValidator(validator);

		List<SyncInvocableHandlerMethod> binderMethods = getBinderMethods(controller);
		List<InvocableHandlerMethod> attributeMethods = getAttributeMethods(controller);

		WebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		BindingContext bindingContext = new InitBinderBindingContext(bindingInitializer, binderMethods);

		this.modelInitializer.initModel(bindingContext, attributeMethods, this.exchange).block(Duration.ofMillis(5000));

		WebExchangeDataBinder binder = bindingContext.createDataBinder(this.exchange, "name");
		assertEquals(Collections.singletonList(validator), binder.getValidators());

		Map<String, Object> model = bindingContext.getModel().asMap();
		assertEquals(5, model.size());

		Object value = model.get("bean");
		assertEquals("Bean", ((TestBean) value).getName());

		value = model.get("monoBean");
		assertEquals("Mono Bean", ((Mono<TestBean>) value).block(Duration.ofMillis(5000)).getName());

		value = model.get("singleBean");
		assertEquals("Single Bean", ((Single<TestBean>) value).toBlocking().value().getName());

		value = model.get("voidMethodBean");
		assertEquals("Void Method Bean", ((TestBean) value).getName());

		value = model.get("voidMonoMethodBean");
		assertEquals("Void Mono Method Bean", ((TestBean) value).getName());
	}

	private List<SyncInvocableHandlerMethod> getBinderMethods(Object controller) {
		return MethodIntrospector
				.selectMethods(controller.getClass(), BINDER_METHODS).stream()
				.map(method -> new SyncInvocableHandlerMethod(controller, method))
				.collect(Collectors.toList());
	}

	private List<InvocableHandlerMethod> getAttributeMethods(Object controller) {
		return MethodIntrospector
				.selectMethods(controller.getClass(), ATTRIBUTE_METHODS).stream()
				.map(method -> toInvocable(controller, method))
				.collect(Collectors.toList());
	}

	private InvocableHandlerMethod toInvocable(Object controller, Method method) {
		ModelArgumentResolver resolver = new ModelArgumentResolver(new ReactiveAdapterRegistry());
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(controller, method);
		handlerMethod.setArgumentResolvers(Collections.singletonList(resolver));
		return handlerMethod;
	}


	@SuppressWarnings("unused")
	private static class TestController {

		private Validator validator;


		void setValidator(Validator validator) {
			this.validator = validator;
		}


		@InitBinder
		public void initDataBinder(WebDataBinder dataBinder) {
			if (this.validator != null) {
				dataBinder.addValidators(this.validator);
			}
		}

		@ModelAttribute("bean")
		public TestBean returnValue() {
			return new TestBean("Bean");
		}

		@ModelAttribute("monoBean")
		public Mono<TestBean> returnValueMono() {
			return Mono.just(new TestBean("Mono Bean"));
		}

		@ModelAttribute("singleBean")
		public Single<TestBean> returnValueSingle() {
			return Single.just(new TestBean("Single Bean"));
		}

		@ModelAttribute
		public void voidMethodBean(Model model) {
			model.addAttribute("voidMethodBean", new TestBean("Void Method Bean"));
		}

		@ModelAttribute
		public Mono<Void> voidMonoMethodBean(Model model) {
			return Mono.just("Void Mono Method Bean")
					.doOnNext(name -> model.addAttribute("voidMonoMethodBean", new TestBean(name)))
					.then();
		}

		@RequestMapping
		public void handle() {}
	}


	private static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return "TestBean[name=" + this.name + "]";
		}
	}

}
