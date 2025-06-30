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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.TestExceptionResolver;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AbstractMethodMessageHandler}.
 *
 * @author Rossen Stoyanchev
 */
class MethodMessageHandlerTests {


	@Test
	void duplicateMapping() {
		assertThatIllegalStateException().isThrownBy(() ->
				initMethodMessageHandler(DuplicateMappingsController.class));
	}

	@Test
	void registeredMappings() {
		TestMethodMessageHandler messageHandler = initMethodMessageHandler(TestController.class);
		Map<String, HandlerMethod> mappings = messageHandler.getHandlerMethods();

		assertThat(mappings).containsOnlyKeys(
				"/handleMessage", "/handleMessageWithArgument", "/handleMessageWithError",
				"/handleMessageMatch1", "/handleMessageMatch2");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void bestMatch() throws NoSuchMethodException {
		TestMethodMessageHandler handler = new TestMethodMessageHandler();
		TestController controller = new TestController();
		handler.registerHandlerMethod(controller,
				TestController.class.getMethod("handleMessageMatch1"), "/bestmatch/{foo}/path");
		handler.registerHandlerMethod(controller,
				TestController.class.getMethod("handleMessageMatch2"), "/bestmatch/*/*");
		handler.afterPropertiesSet();

		Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
				DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
				new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/bestmatch/bar/path")));

		handler.handleMessage(message).block(Duration.ofSeconds(5));

		StepVerifier.create((Publisher<Object>) handler.getLastReturnValue())
				.expectNext("handleMessageMatch1")
				.verifyComplete();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void argumentResolution() {

		ArgumentResolverConfigurer configurer = new ArgumentResolverConfigurer();
		configurer.addCustomResolver(new StubArgumentResolver(String.class, "foo"));

		TestMethodMessageHandler handler = initMethodMessageHandler(
				theHandler -> theHandler.setArgumentResolverConfigurer(configurer),
				TestController.class);

		Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
				DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
				new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/handleMessageWithArgument")));

		handler.handleMessage(message).block(Duration.ofSeconds(5));

		StepVerifier.create((Publisher<Object>) handler.getLastReturnValue())
				.expectNext("handleMessageWithArgument,payload=foo")
				.verifyComplete();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handleException() {

		TestMethodMessageHandler handler = initMethodMessageHandler(TestController.class);

		Message<?> message = new GenericMessage<>("body", Collections.singletonMap(
				DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
				new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("/handleMessageWithError")));

		handler.handleMessage(message).block(Duration.ofSeconds(5));

		StepVerifier.create((Publisher<Object>) handler.getLastReturnValue())
				.expectNext("handleIllegalStateException,ex=rejected")
				.verifyComplete();
	}


	private TestMethodMessageHandler initMethodMessageHandler(Class<?>... handlerTypes) {
		return initMethodMessageHandler(handler -> {}, handlerTypes);
	}

	private TestMethodMessageHandler initMethodMessageHandler(
			Consumer<TestMethodMessageHandler> customizer, Class<?>... handlerTypes) {

		StaticApplicationContext context = new StaticApplicationContext();
		for (Class<?> handlerType : handlerTypes) {
			String beanName = ClassUtils.getShortNameAsProperty(handlerType);
			context.registerPrototype(beanName, handlerType);
		}
		TestMethodMessageHandler messageHandler = new TestMethodMessageHandler();
		messageHandler.setApplicationContext(context);
		customizer.accept(messageHandler);
		messageHandler.afterPropertiesSet();
		return messageHandler;
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public Mono<String> handleMessage() {
			return delay("handleMessage");
		}

		public Mono<String> handleMessageWithArgument(String payload) {
			return delay("handleMessageWithArgument,payload=" + payload);
		}

		public Mono<String> handleMessageWithError() {
			return Mono.delay(Duration.ofMillis(10))
					.flatMap(aLong -> Mono.error(new IllegalStateException("rejected")));
		}

		public Mono<String> handleMessageMatch1() {
			return delay("handleMessageMatch1");
		}

		public Mono<String> handleMessageMatch2() {
			return delay("handleMessageMatch2");
		}

		public Mono<String> handleIllegalStateException(IllegalStateException ex) {
			return delay("handleIllegalStateException,ex=" + ex.getMessage());
		}

		private Mono<String> delay(String value) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> value);
		}
	}


	@SuppressWarnings("unused")
	private static class DuplicateMappingsController {

		void handleMessageFoo() { }

		void handleMessageFoo(String foo) { }
	}


	private static class TestMethodMessageHandler extends AbstractMethodMessageHandler<String> {

		private final TestReturnValueHandler returnValueHandler = new TestReturnValueHandler();

		private PathMatcher pathMatcher = new AntPathMatcher();


		public TestMethodMessageHandler() {
			setHandlerPredicate(handlerType -> handlerType.getName().endsWith("Controller"));
		}

		@Override
		protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
			return Collections.emptyList();
		}

		@Override
		protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
			return Collections.singletonList(this.returnValueHandler);
		}

		public @Nullable Object getLastReturnValue() {
			return this.returnValueHandler.getLastReturnValue();
		}

		@Override
		protected String getMappingForMethod(Method method, Class<?> handlerType) {
			String methodName = method.getName();
			if (methodName.startsWith("handleMessage")) {
				return "/" + methodName;
			}
			return null;
		}

		@Override
		protected Set<String> getDirectLookupMappings(String mapping) {
			return Collections.singleton(mapping);
		}

		@Override
		protected RouteMatcher.@Nullable Route getDestination(Message<?> message) {
			return (RouteMatcher.Route) message.getHeaders().get(
					DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER);
		}

		@Override
		protected String getMatchingMapping(String mapping, Message<?> message) {
			RouteMatcher.Route destination = getDestination(message);
			Assert.notNull(destination, "No destination");
			return mapping.equals(destination.value()) ||
					this.pathMatcher.match(mapping, destination.value()) ? mapping : null;
		}

		@Override
		protected Comparator<String> getMappingComparator(Message<?> message) {
			return (info1, info2) -> {
				SimpleRouteMatcher routeMatcher = new SimpleRouteMatcher(new AntPathMatcher());
				DestinationPatternsMessageCondition cond1 =
						new DestinationPatternsMessageCondition(new String[] { info1 }, routeMatcher);
				DestinationPatternsMessageCondition cond2 =
						new DestinationPatternsMessageCondition(new String[] { info2 }, routeMatcher);
				return cond1.compareTo(cond2, message);
			};
		}

		@Override
		protected AbstractExceptionHandlerMethodResolver createExceptionMethodResolverFor(Class<?> beanType) {
			return new TestExceptionResolver(beanType);
		}
	}

}
