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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveSubscribableChannel;
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
 * Extension of {@link AbstractMethodMessageHandler} for reactive, non-blocking
 * handling of messages via {@link MessageMapping @MessageMapping} methods.
 * By default such methods are detected in {@code @Controller} Spring beans but
 * that can be changed via {@link #setHandlerPredicate(Predicate)}.
 *
 * <p>Payloads for incoming messages are decoded through the configured
 * {@link #setDecoders(List)} decoders, with the help of
 * {@link PayloadMethodArgumentResolver}.
 *
 * <p>There is no default handling for return values but
 * {@link #setReturnValueHandlerConfigurer} can be used to configure custom
 * return value handlers. Sub-classes may also override
 * {@link #initReturnValueHandlers()} to set up default return value handlers.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see AbstractEncoderMethodReturnValueHandler
 */
public class MessageMappingMessageHandler extends AbstractMethodMessageHandler<CompositeMessageCondition>
		implements SmartLifecycle, EmbeddedValueResolverAware {

	private final ReactiveSubscribableChannel inboundChannel;

	private final List<Decoder<?>> decoders = new ArrayList<>();

	@Nullable
	private Validator validator;

	private PathMatcher pathMatcher;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	@Nullable
	private StringValueResolver valueResolver;

	private volatile boolean running = false;

	private final Object lifecycleMonitor = new Object();


	public MessageMappingMessageHandler(ReactiveSubscribableChannel inboundChannel) {
		Assert.notNull(inboundChannel, "`inboundChannel` is required");
		this.inboundChannel = inboundChannel;
		this.pathMatcher = new AntPathMatcher();
		((AntPathMatcher) this.pathMatcher).setPathSeparator(".");
		setHandlerPredicate(beanType -> AnnotatedElementUtils.hasAnnotation(beanType, Controller.class));
	}


	/**
	 * Configure the decoders to use for incoming payloads.
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
	 * Set the Validator instance used for validating {@code @Payload} arguments.
	 * @see org.springframework.validation.annotation.Validated
	 * @see PayloadMethodArgumentResolver
	 */
	public void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	/**
	 * Return the configured Validator instance.
	 */
	@Nullable
	public Validator getValidator() {
		return this.validator;
	}

	/**
	 * Set the PathMatcher implementation to use for matching destinations
	 * against configured destination patterns.
	 * <p>By default, {@link AntPathMatcher} is used with separator set to ".".
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
	 * Configure a {@link ConversionService} to use for type conversion of
	 * String based values, e.g. in destination variables or headers.
	 * <p>By default {@link DefaultFormattingConversionService} is used.
	 * @param conversionService the conversion service to use
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Return the configured ConversionService.
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.valueResolver = resolver;
	}


	@Override
	protected List<? extends HandlerMethodArgumentResolver> initArgumentResolvers() {
		List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

		ApplicationContext context = getApplicationContext();
		ConfigurableBeanFactory beanFactory = (context instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) context).getBeanFactory() : null);

		// Annotation-based resolvers
		resolvers.add(new HeaderMethodArgumentResolver(this.conversionService, beanFactory));
		resolvers.add(new HeadersMethodArgumentResolver());
		resolvers.add(new DestinationVariableMethodArgumentResolver(this.conversionService));

		// Custom resolvers
		resolvers.addAll(getArgumentResolverConfigurer().getCustomResolvers());

		// Catch-all
		resolvers.add(new PayloadMethodArgumentResolver(
				this.decoders, this.validator, getReactiveAdapterRegistry(), true));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		return Collections.emptyList();
	}


	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			this.inboundChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.inboundChannel.unsubscribe(this);
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		return this.running;
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
