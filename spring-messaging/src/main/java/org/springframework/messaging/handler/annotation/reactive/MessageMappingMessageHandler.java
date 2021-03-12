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

package org.springframework.messaging.handler.annotation.reactive;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.KotlinDetector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.AnnotationExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.invocation.reactive.AbstractEncoderMethodReturnValueHandler;
import org.springframework.messaging.handler.invocation.reactive.AbstractMethodMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;
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
		implements EmbeddedValueResolverAware {

	private final List<Decoder<?>> decoders = new ArrayList<>();

	@Nullable
	private Validator validator;

	@Nullable
	private RouteMatcher routeMatcher;

	private ConversionService conversionService = new DefaultFormattingConversionService();

	@Nullable
	private StringValueResolver valueResolver;


	public MessageMappingMessageHandler() {
		setHandlerPredicate(type -> AnnotatedElementUtils.hasAnnotation(type, Controller.class));
	}


	/**
	 * Configure the decoders to use for incoming payloads.
	 */
	public void setDecoders(List<? extends Decoder<?>> decoders) {
		this.decoders.clear();
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
	 * Set the {@code RouteMatcher} to use for mapping messages to handlers
	 * based on the route patterns they're configured with.
	 * <p>By default, {@link SimpleRouteMatcher} is used, backed by
	 * {@link AntPathMatcher} with "." as separator. For greater
	 * efficiency consider using the {@code PathPatternRouteMatcher} from
	 * {@code spring-web} instead.
	 */
	public void setRouteMatcher(@Nullable RouteMatcher routeMatcher) {
		this.routeMatcher = routeMatcher;
	}

	/**
	 * Return the {@code RouteMatcher} used to map messages to handlers.
	 * May be {@code null} before the component is initialized.
	 */
	@Nullable
	public RouteMatcher getRouteMatcher() {
		return this.routeMatcher;
	}

	/**
	 * Obtain the {@code RouteMatcher} for actual use.
	 * @return the RouteMatcher (never {@code null})
	 * @throws IllegalStateException in case of no RouteMatcher set
	 * @since 5.0
	 */
	protected RouteMatcher obtainRouteMatcher() {
		RouteMatcher routeMatcher = getRouteMatcher();
		Assert.state(routeMatcher != null, "No RouteMatcher set");
		return routeMatcher;
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

	/**
	 * Use this method to register a {@link MessagingAdviceBean} that may contain
	 * globally applicable
	 * {@link org.springframework.messaging.handler.annotation.MessageExceptionHandler @MessageExceptionHandler}
	 * methods.
	 * <p>Note: spring-messaging does not depend on spring-web and therefore it
	 * is not possible to explicitly support the registration of a
	 * {@code @ControllerAdvice} bean. You can use the following adapter code
	 * to register {@code @ControllerAdvice} beans here:
	 * <pre>
	 * ControllerAdviceBean.findAnnotatedBeans(context).forEach(bean ->
	 *         messageHandler.registerMessagingAdvice(new ControllerAdviceWrapper(bean));
	 *
	 * public class ControllerAdviceWrapper implements MessagingAdviceBean {
	 *     private final ControllerAdviceBean delegate;
	 *     // delegate all methods
	 * }
	 * </pre>
	 *
	 * @param bean the bean to check for {@code @MessageExceptionHandler} methods
	 * @since 5.3.5
	 */
	public void registerMessagingAdvice(MessagingAdviceBean bean) {
		Class<?> type = bean.getBeanType();
		if (type != null) {
			AnnotationExceptionHandlerMethodResolver resolver = new AnnotationExceptionHandlerMethodResolver(type);
			if (resolver.hasExceptionMappings()) {
				registerExceptionHandlerAdvice(bean, resolver);
				if (logger.isTraceEnabled()) {
					logger.trace("Detected @MessageExceptionHandler methods in " + bean);
				}
			}
		}
	}

	@Override
	public void afterPropertiesSet() {

		// Initialize RouteMatcher before parent initializes handler mappings
		if (this.routeMatcher == null) {
			AntPathMatcher pathMatcher = new AntPathMatcher();
			pathMatcher.setPathSeparator(".");
			this.routeMatcher = new SimpleRouteMatcher(pathMatcher);
		}

		super.afterPropertiesSet();
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

		// Type-based...
		if (KotlinDetector.isKotlinPresent()) {
			resolvers.add(new ContinuationHandlerMethodArgumentResolver());
		}

		// Custom resolvers
		resolvers.addAll(getArgumentResolverConfigurer().getCustomResolvers());

		// Catch-all
		resolvers.add(new PayloadMethodArgumentResolver(
				getDecoders(), this.validator, getReactiveAdapterRegistry(), true));

		return resolvers;
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		return Collections.emptyList();
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

	/**
	 * Determine the mapping condition for the given annotated element.
	 * @param element the element to check
	 * @return the condition, or {@code null}
	 */
	@Nullable
	protected CompositeMessageCondition getCondition(AnnotatedElement element) {
		MessageMapping ann = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
		if (ann == null || ann.value().length == 0) {
			return null;
		}
		String[] patterns = processDestinations(ann.value());
		return new CompositeMessageCondition(
				new DestinationPatternsMessageCondition(patterns, obtainRouteMatcher()));
	}

	/**
	 * Resolve placeholders in the given destinations.
	 * @param destinations the destinations
	 * @return new array with the processed destinations or the same array
	 */
	protected String[] processDestinations(String[] destinations) {
		if (this.valueResolver != null) {
			destinations = Arrays.stream(destinations)
					.map(s -> this.valueResolver.resolveStringValue(s))
					.toArray(String[]::new);
		}
		return destinations;
	}

	@Override
	protected Set<String> getDirectLookupMappings(CompositeMessageCondition mapping) {
		Set<String> result = new LinkedHashSet<>();
		for (String pattern : mapping.getCondition(DestinationPatternsMessageCondition.class).getPatterns()) {
			if (!obtainRouteMatcher().isPattern(pattern)) {
				result.add(pattern);
			}
		}
		return result;
	}

	@Override
	protected RouteMatcher.Route getDestination(Message<?> message) {
		return (RouteMatcher.Route) message.getHeaders()
				.get(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER);
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

	@Override
	protected Mono<Void> handleMatch(
			CompositeMessageCondition mapping, HandlerMethod handlerMethod, Message<?> message) {

		Set<String> patterns = mapping.getCondition(DestinationPatternsMessageCondition.class).getPatterns();
		if (!CollectionUtils.isEmpty(patterns)) {
			String pattern = patterns.iterator().next();
			RouteMatcher.Route destination = getDestination(message);
			Assert.state(destination != null, "Missing destination header");
			Map<String, String> vars = obtainRouteMatcher().matchAndExtract(pattern, destination);
			if (!CollectionUtils.isEmpty(vars)) {
				MessageHeaderAccessor mha = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
				Assert.state(mha != null && mha.isMutable(), "Mutable MessageHeaderAccessor required");
				mha.setHeader(DestinationVariableMethodArgumentResolver.DESTINATION_TEMPLATE_VARIABLES_HEADER, vars);
			}
		}
		return super.handleMatch(mapping, handlerMethod, message);
	}

}
