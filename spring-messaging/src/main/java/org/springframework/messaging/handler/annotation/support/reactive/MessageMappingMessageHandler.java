/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.messaging.handler.annotation.support.reactive;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.reactive.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringValueResolver;
import org.springframework.validation.Validator;

/**
 * Extension of {@link AbstractMethodMessageHandler} for
 * {@link MessageMapping @MessageMapping} methods.
 *
 * <p>The payload of incoming messages is decoded through
 * {@link PayloadMethodArgumentResolver} using one of the configured
 * {@link #setDecoders(List)} decoders.
 *
 * <p>The {@link #setEncoderReturnValueHandler encoderReturnValueHandler}
 * property must be set to encode and handle return values from
 * {@code @MessageMapping} methods.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class MessageMappingMessageHandler extends AbstractMethodMessageHandler<CompositeMessageCondition>
		implements EmbeddedValueResolverAware {

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final List<Decoder<?>> decoders = new ArrayList<>();

	@Nullable
	private Validator validator;

	@Nullable
	private HandlerMethodReturnValueHandler encoderReturnValueHandler;

	@Nullable
	private StringValueResolver valueResolver;


	/**
	 * Set the PathMatcher implementation to use for matching destinations
	 * against configured destination patterns.
	 * <p>By default, {@link AntPathMatcher} is used.
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		Assert.notNull(pathMatcher, "PathMatcher must not be null");
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Return the PathMatcher implementation to use for matching destinations.
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * Configure the decoders to user for incoming payloads.
	 */
	public void setDecoders(List<? extends Decoder<?>> decoders) {
		this.decoders.addAll(decoders);
	}

	/**
	 * Return the configured decoders.
	 */
	public List<? extends Decoder<?>> getDecoders() {
		return this.decoders;
	}

	/**
	 * Return the configured Validator instance.
	 */
	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * Set the Validator instance used for validating {@code @Payload} arguments.
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadMethodArgumentResolver
	 */
	public void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	/**
	 * Configure the return value handler that will encode response content.
	 * Consider extending {@link AbstractEncoderMethodReturnValueHandler} which
	 * provides the infrastructure to encode and all that's left is to somehow
	 * handle the encoded content, e.g. by wrapping as a message and passing it
	 * to something or sending it somewhere.
	 * <p>By default this is not configured in which case payload/content return
	 * values from {@code @MessageMapping} methods will remain unhandled.
	 * @param encoderReturnValueHandler the return value handler to use
	 * @see AbstractEncoderMethodReturnValueHandler
	 */
	public void setEncoderReturnValueHandler(@Nullable HandlerMethodReturnValueHandler encoderReturnValueHandler) {
		this.encoderReturnValueHandler = encoderReturnValueHandler;
	}

	/**
	 * Return the configured
	 * {@link #setEncoderReturnValueHandler encoderReturnValueHandler}.
	 */
	@Nullable
	public HandlerMethodReturnValueHandler getEncoderReturnValueHandler() {
		return this.encoderReturnValueHandler;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.valueResolver = resolver;
	}


	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		// Custom resolvers
		resolvers.addAll(getArgumentResolverConfigurer().getCustomResolvers());

		// Catch-all
		resolvers.add(new PayloadMethodArgumentResolver(
				this.decoders, this.validator, getReactiveAdapterRegistry(), true));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
		handlers.addAll(getReturnValueHandlerConfigurer().getCustomHandlers());
		if (this.encoderReturnValueHandler != null) {
			handlers.add(this.encoderReturnValueHandler);
		}
		return handlers;
	}


	@Override
	protected boolean isHandler(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
	}

	@Override
	protected CompositeMessageCondition getMappingForMethod(Method method, Class<?> handlerType) {
		CompositeMessageCondition methodCondition = getCondition(method);
		if (methodCondition != null) {
			CompositeMessageCondition typeCondition = getCondition(handlerType);
			if (typeCondition != null) {
				return typeCondition.combine(methodCondition);
			}
		}
		return methodCondition;
	}

	@Nullable
	private CompositeMessageCondition getCondition(AnnotatedElement element) {
		MessageMapping annot = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
		if (annot == null || annot.value().length == 0) {
			return null;
		}
		String[] destinations = annot.value();
		if (this.valueResolver != null) {
			destinations = Arrays.stream(annot.value())
					.map(s -> this.valueResolver.resolveStringValue(s))
					.toArray(String[]::new);
		}
		return new CompositeMessageCondition(new DestinationPatternsMessageCondition(destinations, this.pathMatcher));
	}

	@Override
	protected Set<String> getDirectLookupMappings(CompositeMessageCondition mapping) {
		Set<String> result = new LinkedHashSet<>();
		for (String pattern : mapping.getCondition(DestinationPatternsMessageCondition.class).getPatterns()) {
			if (!this.pathMatcher.isPattern(pattern)) {
				result.add(pattern);
			}
		}
		return result;
	}

	@Override
	protected String getDestination(Message<?> message) {
		return (String) message.getHeaders().get(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER);
	}

	@Override
	protected CompositeMessageCondition getMatchingMapping(CompositeMessageCondition mapping, Message<?> message) {
		return mapping.getMatchingCondition(message);
	}

	@Override
	protected Comparator<CompositeMessageCondition> getMappingComparator(Message<?> message) {
		return (info1, info2) -> info1.compareTo(info2, message);
	}

	@Override
	protected AbstractExceptionHandlerMethodResolver createExceptionMethodResolverFor(Class<?> beanType) {
		return new AnnotationExceptionHandlerMethodResolver(beanType);
	}

}
