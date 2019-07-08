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
package org.springframework.messaging.rsocket.annotation.support;

import java.util.Collections;
import java.util.Map;

import io.rsocket.frame.FrameType;
import org.junit.Test;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RSocketMessageHandler}.
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketMessageHandlerTests {

	@Test
	public void mappings() {
		testMapping(new SimpleController(), "path");
		testMapping(new TypeLevelMappingController(), "base.path");
		testMapping(new HandleAllController());
	}

	private static void testMapping(Object controller, String... expectedPatterns) {
		RSocketMessageHandler handler = new RSocketMessageHandler();
		handler.setDecoders(Collections.singletonList(StringDecoder.allMimeTypes()));
		handler.setEncoders(Collections.singletonList(CharSequenceEncoder.allMimeTypes()));
		handler.setHandlers(Collections.singletonList(controller));
		handler.afterPropertiesSet();

		Map<CompositeMessageCondition, HandlerMethod> map = handler.getHandlerMethods();
		assertThat(map).hasSize(1);

		CompositeMessageCondition condition = map.entrySet().iterator().next().getKey();
		RSocketFrameTypeMessageCondition c1 = condition.getCondition(RSocketFrameTypeMessageCondition.class);
		assertThat(c1.getFrameTypes()).contains(FrameType.SETUP, FrameType.METADATA_PUSH);

		DestinationPatternsMessageCondition c2 = condition.getCondition(DestinationPatternsMessageCondition.class);
		if (ObjectUtils.isEmpty(expectedPatterns)) {
			assertThat(c2.getPatterns()).isEmpty();
		}
		else {
			assertThat(c2.getPatterns()).contains(expectedPatterns);
		}
	}

	@Test
	public void handleNoMatch() {

		testHandleNoMatch(FrameType.SETUP);
		testHandleNoMatch(FrameType.METADATA_PUSH);
		testHandleNoMatch(FrameType.REQUEST_FNF);

		assertThatThrownBy(() -> testHandleNoMatch(FrameType.REQUEST_RESPONSE))
			.hasMessage("No handler for destination 'path'");
	}

	private static void testHandleNoMatch(FrameType frameType) {
		RSocketMessageHandler handler = new RSocketMessageHandler();
		handler.setDecoders(Collections.singletonList(StringDecoder.allMimeTypes()));
		handler.setEncoders(Collections.singletonList(CharSequenceEncoder.allMimeTypes()));
		handler.afterPropertiesSet();

		RouteMatcher matcher = new SimpleRouteMatcher(new AntPathMatcher("."));
		RouteMatcher.Route route = matcher.parseRoute("path");

		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.setHeader(RSocketFrameTypeMessageCondition.FRAME_TYPE_HEADER, frameType);
		Message<Object> message = MessageBuilder.createMessage("", headers.getMessageHeaders());

		handler.handleNoMatch(route, message);
	}


	private static class SimpleController {

		@ConnectMapping("path")
		public void handle() {
		}
	}


	@MessageMapping("base")
	private static class TypeLevelMappingController {

		@ConnectMapping("path")
		public void handleWithPatterns() {
		}
	}


	private static class HandleAllController {

		@ConnectMapping
		public void handleAll() {
		}
	}

}
