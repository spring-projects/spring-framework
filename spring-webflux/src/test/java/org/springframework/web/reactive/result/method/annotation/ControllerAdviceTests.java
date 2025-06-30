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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@code @ControllerAdvice} related tests for {@link RequestMappingHandlerAdapter}.
 *
 * @author Rossen Stoyanchev
 */
class ControllerAdviceTests {

	private final MockServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private final MockServerWebExchange postExchange =
			MockServerWebExchange.from(MockServerHttpRequest.post("/")
					.body(Flux.defer(() -> {
						byte[] bytes = "request body".getBytes();
						DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
						return Flux.just(buffer).subscribeOn(Schedulers.newSingle("bad thread"));
					})));


	@Test
	void resolveExceptionGlobalHandler() throws Exception {
		testException(new IllegalAccessException(), "SecondControllerAdvice: IllegalAccessException");
	}

	@Test
	void resolveExceptionGlobalHandlerOrdered() throws Exception {
		testException(new IllegalStateException(), "OneControllerAdvice: IllegalStateException");
	}

	@Test // SPR-12605
	public void resolveExceptionWithHandlerMethodArg() throws Exception {
		testException(new ArrayIndexOutOfBoundsException(), "HandlerMethod: handle");
	}

	@Test
	void resolveExceptionWithAssertionError() throws Exception {
		AssertionError error = new AssertionError("argh");
		testException(error, error.toString());
	}

	@Test
	void resolveExceptionWithAssertionErrorAsRootCause() throws Exception {
		AssertionError rootCause = new AssertionError("argh");
		FatalBeanException cause = new FatalBeanException("wrapped", rootCause);
		Exception exception = new Exception(cause);
		testException(exception, rootCause.toString());
	}

	@Test
	void resolveOnAnotherThread() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		final RequestMappingHandlerAdapter adapter = createAdapterWithExecutor(context, "good thread");

		TestController controller = context.getBean(TestController.class);

		Object actual = handle(adapter, controller, this.postExchange, Duration.ofMillis(100),
				"threadWithArg", String.class).getReturnValue();
		assertThat(actual).isEqualTo("request body from good thread");
	}

	@Test
	void resolveEmptyOnAnotherThread() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		final RequestMappingHandlerAdapter adapter = createAdapterWithExecutor(context, "good thread");

		TestController controller = context.getBean(TestController.class);

		Object actual = handle(adapter, controller, this.postExchange, Duration.ofMillis(100), "thread")
				.getReturnValue();
		assertThat(actual).isEqualTo("hello from good thread");
	}

	@Test
	void resolveExceptionOnAnotherThread() throws Exception {
		testException(new IllegalArgumentException(), "good thread",
				"OneControllerAdvice: IllegalArgumentException on thread good thread");
	}

	@Test
	void modelAttributeAdvice() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		RequestMappingHandlerAdapter adapter = createAdapter(context);
		TestController controller = context.getBean(TestController.class);

		Model model = handle(adapter, controller, "handle").getModel();

		assertThat(model.asMap()).hasSize(2);
		assertThat(model.asMap().get("attr1")).isEqualTo("lAttr1");
		assertThat(model.asMap().get("attr2")).isEqualTo("gAttr2");
	}

	@Test
	void initBinderAdvice() throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		RequestMappingHandlerAdapter adapter = createAdapter(context);
		TestController controller = context.getBean(TestController.class);

		Validator validator = mock();
		controller.setValidator(validator);

		BindingContext bindingContext = handle(adapter, controller, "handle").getBindingContext();

		WebExchangeDataBinder binder = bindingContext.createDataBinder(this.exchange, "name");
		assertThat(binder.getValidators()).isEqualTo(Collections.singletonList(validator));
	}


	private void testException(Throwable exception, String expected) throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		RequestMappingHandlerAdapter adapter = createAdapter(context);

		TestController controller = context.getBean(TestController.class);
		controller.setException(exception);

		Object actual = handle(adapter, controller, "handle").getReturnValue();
		assertThat(actual).isEqualTo(expected);
	}

	private RequestMappingHandlerAdapter createAdapter(ApplicationContext context) throws Exception {
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.setApplicationContext(context);
		adapter.afterPropertiesSet();
		return adapter;
	}

	private void testException(Throwable exception, String threadName, String expected) throws Exception {
		ApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		RequestMappingHandlerAdapter adapter = createAdapterWithExecutor(context, threadName);

		TestController controller = context.getBean(TestController.class);
		controller.setException(exception);

		Object actual = handle(adapter, controller, this.postExchange, Duration.ofMillis(1000),
				"threadWithArg", String.class).getReturnValue();
		assertThat(actual).isEqualTo(expected);
	}

	private RequestMappingHandlerAdapter createAdapterWithExecutor(ApplicationContext context, String threadName) throws Exception {
		RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
		adapter.setApplicationContext(context);
		adapter.setBlockingExecutor(Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r);
			t.setName(threadName);
			return t;
		}));
		adapter.setBlockingMethodPredicate(m -> true);
		adapter.afterPropertiesSet();
		return adapter;
	}

	private HandlerResult handle(RequestMappingHandlerAdapter adapter,
			Object controller, String methodName, Class<?>... parameterTypes) throws Exception {
		return handle(adapter, controller, this.exchange, Duration.ZERO, methodName, parameterTypes);
	}

	private HandlerResult handle(RequestMappingHandlerAdapter adapter,
			Object controller, ServerWebExchange exchange, Duration timeout,
			String methodName, Class<?>... parameterTypes) throws Exception {

		Method method = controller.getClass().getMethod(methodName, parameterTypes);
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		HandlerResult handlerResult = adapter.handle(exchange, handlerMethod).block(timeout);
		assertThat(handlerResult).isNotNull();
		return handlerResult;
	}


	@Configuration
	static class TestConfig {

		@Bean
		public TestController testController() {
			return new TestController();
		}

		@Bean
		public OneControllerAdvice testExceptionResolver() {
			return new OneControllerAdvice();
		}

		@Bean
		public SecondControllerAdvice anotherTestExceptionResolver() {
			return new SecondControllerAdvice();
		}
	}


	@Controller
	static class TestController {

		private Validator validator;

		private Throwable exception;

		void setValidator(Validator validator) {
			this.validator = validator;
		}

		void setException(Throwable exception) {
			this.exception = exception;
		}

		@InitBinder
		public void initDataBinder(WebDataBinder dataBinder) {
			if (this.validator != null) {
				dataBinder.addValidators(this.validator);
			}
		}

		@ModelAttribute
		public void addAttributes(Model model) {
			model.addAttribute("attr1", "lAttr1");
		}

		@GetMapping
		public void handle() throws Throwable {
			if (this.exception != null) {
				throw this.exception;
			}
		}

		@PostMapping
		public String threadWithArg(@RequestBody String body) throws Throwable {
			handle();
			return body + " from " + Thread.currentThread().getName();
		}

		@GetMapping
		public String thread() throws Throwable {
			handle();
			return "hello from " + Thread.currentThread().getName();
		}
	}


	@ControllerAdvice
	@Order(1)
	static class OneControllerAdvice {

		@ModelAttribute
		public void addAttributes(Model model) {
			model.addAttribute("attr1", "gAttr1");
			model.addAttribute("attr2", "gAttr2");
		}

		@ExceptionHandler
		public String handleException(IllegalStateException ex) {
			return "OneControllerAdvice: " + ClassUtils.getShortName(ex.getClass());
		}

		@ExceptionHandler(ArrayIndexOutOfBoundsException.class)
		public String handleWithHandlerMethod(HandlerMethod handlerMethod) {
			return "HandlerMethod: " + handlerMethod.getMethod().getName();
		}

		@ExceptionHandler(IllegalArgumentException.class)
		public String handleOnThread(IllegalArgumentException ex) {
			return "OneControllerAdvice: " + ClassUtils.getShortName(ex.getClass()) +
					" on thread " + Thread.currentThread().getName();
		}

		@ExceptionHandler(AssertionError.class)
		public String handleAssertionError(Error err) {
			return err.toString();
		}
	}


	@ControllerAdvice
	@Order(2)
	static class SecondControllerAdvice {

		@ExceptionHandler({IllegalStateException.class, IllegalAccessException.class})
		public String handleException(Exception ex) {
			return "SecondControllerAdvice: " + ClassUtils.getShortName(ex.getClass());
		}
	}

}
