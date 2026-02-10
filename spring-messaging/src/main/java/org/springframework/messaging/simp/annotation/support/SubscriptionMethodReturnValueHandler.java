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

package org.springframework.messaging.simp.annotation.support;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * {@code HandlerMethodReturnValueHandler} for replying directly to a
 * subscription. It is supported on methods annotated with
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping
 * SubscribeMapping} such that the return value is treated as a response to be
 * sent directly back on the session. This allows a client to implement
 * a request-response pattern and use it for example to obtain some data upon
 * initialization.
 *
 * <p>The value returned from the method is converted and turned into a
 * {@link Message} that is then enriched with the sessionId, subscriptionId, and
 * destination of the input message.
 *
 * <p><strong>Note:</strong> this default behavior for interpreting the return
 * value from an {@code @SubscribeMapping} method can be overridden through use
 * of the {@link SendTo} or {@link SendToUser} annotations in which case a
 * message is prepared and sent to the broker instead.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class SubscriptionMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	private static final Log logger = SimpLogging.forLogName(SubscriptionMethodReturnValueHandler.class);


	private final MessageSendingOperations<String> messagingTemplate;

	private @Nullable MessageHeaderInitializer headerInitializer;

	private @Nullable Predicate<String> headerFilter;


	/**
	 * Construct a new SubscriptionMethodReturnValueHandler.
	 * @param template a messaging template to send messages to,
	 * most likely the "clientOutboundChannel" (must not be {@code null})
	 */
	public SubscriptionMethodReturnValueHandler(MessageSendingOperations<String> template) {
		Assert.notNull(template, "messagingTemplate must not be null");
		this.messagingTemplate = template;
	}


	/**
	 * Configure a {@link MessageHeaderInitializer} to apply to the headers of all
	 * messages sent to the client outbound channel.
	 * <p>By default this property is not set.
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured header initializer.
	 */
	public @Nullable MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	/**
	 * Add a filter to determine which headers from the input message should be
	 * propagated to the output message. The filter is applied to the "native
	 * headers" submap. Multiple filters are combined with
	 * {@link Predicate#or(Predicate)}.
	 * <p>By default, no headers are propagated if this is not set.
	 * @since 7.0.4
	 */
	public void addHeaderFilter(Predicate<String> filter) {
		Assert.notNull(filter, "Filter predicate must not be null");
		this.headerFilter = (this.headerFilter != null ? this.headerFilter.or(filter) : filter);
	}

	/**
	 * Return the configured header filter.
	 * @since 7.0.4
	 */
	public @Nullable Predicate<String> getHeaderFilter() {
		return this.headerFilter;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (returnType.hasMethodAnnotation(SubscribeMapping.class) &&
				!returnType.hasMethodAnnotation(SendTo.class) &&
				!returnType.hasMethodAnnotation(SendToUser.class));
	}

	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, Message<?> message)
			throws Exception {

		if (returnValue == null) {
			return;
		}

		MessageHeaders headers = message.getHeaders();
		String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
		String subscriptionId = SimpMessageHeaderAccessor.getSubscriptionId(headers);
		String destination = SimpMessageHeaderAccessor.getDestination(headers);

		if (subscriptionId == null) {
			throw new IllegalStateException("No simpSubscriptionId in " + message +
					" returned by: " + returnType.getMethod());
		}
		if (destination == null) {
			throw new IllegalStateException("No simpDestination in " + message +
					" returned by: " + returnType.getMethod());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Reply to @SubscribeMapping: " + returnValue);
		}
		MessageHeaders headersToSend = createHeaders(sessionId, subscriptionId, returnType, message);
		this.messagingTemplate.convertAndSend(destination, returnValue, headersToSend);
	}

	private MessageHeaders createHeaders(
			@Nullable String sessionId, String subscriptionId, MethodParameter returnType,
			@Nullable Message<?> inputMessage) {

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(accessor);
		}
		if (inputMessage != null && this.headerFilter != null) {
			getNativeHeaders(inputMessage).forEach((name, values) -> {
				if (this.headerFilter.test(name)) {
					accessor.setNativeHeaderValues(name, values);
				}
			});
		}
		if (sessionId != null) {
			accessor.setSessionId(sessionId);
		}
		accessor.setSubscriptionId(subscriptionId);
		accessor.setHeader(AbstractMessageSendingTemplate.CONVERSION_HINT_HEADER, returnType);
		accessor.setLeaveMutable(true);
		return accessor.getMessageHeaders();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<String>> getNativeHeaders(Message<?> message) {
		Object value = message.getHeaders().get(NativeMessageHeaderAccessor.NATIVE_HEADERS);
		return (value != null ? (Map<String, List<String>>) value : Collections.emptyMap());
	}

}
