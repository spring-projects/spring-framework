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
package org.springframework.messaging.handler.annotation.reactive;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.invocation.reactive.TestEncoderMethodReturnValueHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.SimpleRouteMatcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MessageMappingMessageHandler}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("ALL")
public class MessageMappingMessageHandlerTests {

	private TestEncoderMethodReturnValueHandler returnValueHandler;


	@Test
	public void handleString() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("string", "abcdef")).block(Duration.ofSeconds(5));
		verifyOutputContent(Collections.singletonList("abcdef::response"));
	}

	@Test
	public void handleMonoString() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("monoString", "abcdef")).block(Duration.ofSeconds(5));
		verifyOutputContent(Collections.singletonList("abcdef::response"));
	}

	@Test
	public void handleFluxString() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("fluxString", "abc", "def", "ghi")).block(Duration.ofSeconds(5));
		verifyOutputContent(Arrays.asList("abc::response", "def::response", "ghi::response"));
	}

	@Test
	public void handleWithPlaceholderInMapping() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("path123", "abcdef")).block(Duration.ofSeconds(5));
		verifyOutputContent(Collections.singletonList("abcdef::response"));
	}

	@Test
	public void handleWithDestinationVariable() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("destination.test", "abcdef")).block(Duration.ofSeconds(5));
		verifyOutputContent(Collections.singletonList("test::abcdef::response"));
	}

	@Test
	public void handleException() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("exception", "abc")).block(Duration.ofSeconds(5));
		verifyOutputContent(Collections.singletonList("rejected::handled"));
	}

	@Test
	public void handleErrorSignal() {
		MessageMappingMessageHandler messsageHandler = initMesssageHandler();
		messsageHandler.handleMessage(message("errorSignal", "abc")).block(Duration.ofSeconds(5));
		verifyOutputContent(Collections.singletonList("rejected::handled"));
	}

	@Test
	public void unhandledExceptionShouldFlowThrough() {

		GenericMessage<?> message = new GenericMessage<>(new Object(),
				Collections.singletonMap(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
						new SimpleRouteMatcher(new AntPathMatcher()).parseRoute("string")));

		StepVerifier.create(initMesssageHandler().handleMessage(message))
				.expectErrorSatisfies(ex -> assertThat(ex.getMessage().startsWith("Could not resolve method parameter at index 0")).as("Actual: " + ex.getMessage()).isTrue())
				.verify(Duration.ofSeconds(5));
	}


	private MessageMappingMessageHandler initMesssageHandler() {

		List<Decoder<?>> decoders = Collections.singletonList(StringDecoder.allMimeTypes());
		List<Encoder<?>> encoders = Collections.singletonList(CharSequenceEncoder.allMimeTypes());

		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		this.returnValueHandler = new TestEncoderMethodReturnValueHandler(encoders, registry);

		PropertySource<?> source = new MapPropertySource("test", Collections.singletonMap("path", "path123"));

		StaticApplicationContext context = new StaticApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(source);
		context.registerSingleton("testController", TestController.class);
		context.refresh();

		MessageMappingMessageHandler messageHandler = new MessageMappingMessageHandler();
		messageHandler.getReturnValueHandlerConfigurer().addCustomHandler(this.returnValueHandler);
		messageHandler.setApplicationContext(context);
		messageHandler.setEmbeddedValueResolver(new EmbeddedValueResolver(context.getBeanFactory()));
		messageHandler.setDecoders(decoders);
		messageHandler.afterPropertiesSet();

		return messageHandler;
	}

	private Message<?> message(String destination, String... content) {
		Flux<DataBuffer> payload = Flux.fromIterable(Arrays.asList(content)).map(parts -> toDataBuffer(parts));
		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.setLeaveMutable(true);
		headers.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER,
				new SimpleRouteMatcher(new AntPathMatcher()).parseRoute(destination));
		return MessageBuilder.createMessage(payload, headers.getMessageHeaders());
	}

	private DataBuffer toDataBuffer(String payload) {
		return DefaultDataBufferFactory.sharedInstance.wrap(payload.getBytes(UTF_8));
	}

	private void verifyOutputContent(List<String> expected) {
		Flux<String> result = this.returnValueHandler.getContentAsStrings();
		StepVerifier.create(result.collectList()).expectNext(expected).verifyComplete();
	}


	@Controller
	static class TestController {

		@MessageMapping("string")
		String handleString(String payload) {
			return payload + "::response";
		}

		@MessageMapping("monoString")
		Mono<String> handleMonoString(Mono<String> payload) {
			return payload.map(s -> s + "::response").delayElement(Duration.ofMillis(10));
		}

		@MessageMapping("fluxString")
		Flux<String> handleFluxString(Flux<String> payload) {
			return payload.map(s -> s + "::response").delayElements(Duration.ofMillis(10));
		}

		@MessageMapping("${path}")
		String handleWithPlaceholder(String payload) {
			return payload + "::response";
		}

		@MessageMapping("destination.{variable}")
		String handleWithDestinationVariable(@DestinationVariable String variable, String payload) {
			return variable + "::" + payload + "::response";
		}

		@MessageMapping("exception")
		String handleAndThrow() {
			throw new IllegalArgumentException("rejected");
		}

		@MessageMapping("errorSignal")
		Mono<String> handleAndSignalError() {
			return Mono.delay(Duration.ofMillis(10))
					.flatMap(aLong -> Mono.error(new IllegalArgumentException("rejected")));
		}

		@MessageExceptionHandler
		Mono<String> handleException(IllegalArgumentException ex) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> ex.getMessage() + "::handled");
		}
	}

}
