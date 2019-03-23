/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.messaging.handler.MessagingAdviceBean;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * Abstract base class for HandlerMethod-based message handling. Provides most of
 * the logic required to discover handler methods at startup, find a matching handler
 * method at runtime for a given message and invoke it.
 *
 * <p>Also supports discovering and invoking exception handling methods to process
 * exceptions raised during message handling.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 * @param <T> the type of the Object that contains information mapping a
 * {@link org.springframework.messaging.handler.HandlerMethod} to incoming messages
 */
public abstract class AbstractMethodMessageHandler<T>
		implements MessageHandler, ApplicationContextAware, InitializingBean {

	/**
	 * Bean name prefix for target beans behind scoped proxies. Used to exclude those
	 * targets from handler method detection, in favor of the corresponding proxies.
	 * <p>We're not checking the autowire-candidate status here, which is how the
	 * proxy target filtering problem is being handled at the autowiring level,
	 * since autowire-candidate may have been turned to {@code false} for other
	 * reasons, while still expecting the bean to be eligible for handler methods.
	 * <p>Originally defined in {@link org.springframework.aop.scope.ScopedProxyUtils}
	 * but duplicated here to avoid a hard dependency on the spring-aop module.
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Log handlerMethodLogger;


	private final List<String> destinationPrefixes = new ArrayList<>();

	private final List<HandlerMethodArgumentResolver> customArgumentResolvers = new ArrayList<>(4);

	private final List<HandlerMethodReturnValueHandler> customReturnValueHandlers = new ArrayList<>(4);

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final HandlerMethodReturnValueHandlerComposite returnValueHandlers =
			new HandlerMethodReturnValueHandlerComposite();

	@Nullable
	private ApplicationContext applicationContext;

	private final Map<T, HandlerMethod> handlerMethods = new LinkedHashMap<>(64);

	private final MultiValueMap<String, T> destinationLookup = new LinkedMultiValueMap<>(64);

	private final Map<Class<?>, AbstractExceptionHandlerMethodResolver> exceptionHandlerCache =
			new ConcurrentHashMap<>(64);

	private final Map<MessagingAdviceBean, AbstractExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
			new LinkedHashMap<>(64);


	/**
	 * When this property is configured only messages to destinations matching
	 * one of the configured prefixes are eligible for handling. When there is a
	 * match the prefix is removed and only the remaining part of the destination
	 * is used for method-mapping purposes.
	 * <p>By default, no prefixes are configured in which case all messages are
	 * eligible for handling.
	 */
	public void setDestinationPrefixes(@Nullable Collection<String> prefixes) {
		this.destinationPrefixes.clear();
		if (prefixes != null) {
			for (String prefix : prefixes) {
				prefix = prefix.trim();
				this.destinationPrefixes.add(prefix);
			}
		}
	}

	/**
	 * Return the configured destination prefixes, if any.
	 */
	public Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}

	/**
	 * Sets the list of custom {@code HandlerMethodArgumentResolver}s that will be used
	 * after resolvers for supported argument type.
	 */
	public void setCustomArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> customArgumentResolvers) {
		this.customArgumentResolvers.clear();
		if (customArgumentResolvers != null) {
			this.customArgumentResolvers.addAll(customArgumentResolvers);
		}
	}

	/**
	 * Return the configured custom argument resolvers, if any.
	 */
	public List<HandlerMethodArgumentResolver> getCustomArgumentResolvers() {
		return this.customArgumentResolvers;
	}

	/**
	 * Set the list of custom {@code HandlerMethodReturnValueHandler}s that will be used
	 * after return value handlers for known types.
	 */
	public void setCustomReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> customReturnValueHandlers) {
		this.customReturnValueHandlers.clear();
		if (customReturnValueHandlers != null) {
			this.customReturnValueHandlers.addAll(customReturnValueHandlers);
		}
	}

	/**
	 * Return the configured custom return value handlers, if any.
	 */
	public List<HandlerMethodReturnValueHandler> getCustomReturnValueHandlers() {
		return this.customReturnValueHandlers;
	}

	/**
	 * Configure the complete list of supported argument types, effectively overriding
	 * the ones configured by default. This is an advanced option; for most use cases
	 * it should be sufficient to use {@link #setCustomArgumentResolvers}.
	 */
	public void setArgumentResolvers(@Nullable List<HandlerMethodArgumentResolver> argumentResolvers) {
		if (argumentResolvers == null) {
			this.argumentResolvers.clear();
			return;
		}
		this.argumentResolvers.addResolvers(argumentResolvers);
	}

	/**
	 * Return the complete list of argument resolvers.
	 */
	public List<HandlerMethodArgumentResolver> getArgumentResolvers() {
		return this.argumentResolvers.getResolvers();
	}

	/**
	 * Configure the complete list of supported return value types, effectively overriding
	 * the ones configured by default. This is an advanced option; for most use cases
	 * it should be sufficient to use {@link #setCustomReturnValueHandlers}.
	 */
	public void setReturnValueHandlers(@Nullable List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		if (returnValueHandlers == null) {
			this.returnValueHandlers.clear();
			return;
		}
		this.returnValueHandlers.addHandlers(returnValueHandlers);
	}

	/**
	 * Return the complete list of return value handlers.
	 */
	public List<HandlerMethodReturnValueHandler> getReturnValueHandlers() {
		return this.returnValueHandlers.getReturnValueHandlers();
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.argumentResolvers.getResolvers().isEmpty()) {
			this.argumentResolvers.addResolvers(initArgumentResolvers());
		}

		if (this.returnValueHandlers.getReturnValueHandlers().isEmpty()) {
			this.returnValueHandlers.addHandlers(initReturnValueHandlers());
		}
		Log returnValueLogger = getReturnValueHandlerLogger();
		if (returnValueLogger != null) {
			this.returnValueHandlers.setLogger(returnValueLogger);
		}

		this.handlerMethodLogger = getHandlerMethodLogger();

		ApplicationContext context = getApplicationContext();
		if (context == null) {
			return;
		}
		for (String beanName : context.getBeanNamesForType(Object.class)) {
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = context.getType(beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
					detectHandlerMethods(beanName);
				}
			}
		}
	}

	/**
	 * Return the list of argument resolvers to use. Invoked only if the resolvers
	 * have not already been set via {@link #setArgumentResolvers}.
	 * <p>Subclasses should also take into account custom argument types configured via
	 * {@link #setCustomArgumentResolvers}.
	 */
	protected abstract List<? extends HandlerMethodArgumentResolver> initArgumentResolvers();

	/**
	 * Return the list of return value handlers to use. Invoked only if the return
	 * value handlers have not already been set via {@link #setReturnValueHandlers}.
	 * <p>Subclasses should also take into account custom return value types configured
	 * via {@link #setCustomReturnValueHandlers}.
	 */
	protected abstract List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers();


	/**
	 * Whether the given bean type should be introspected for messaging handling methods.
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * Detect if the given handler has any methods that can handle messages and if
	 * so register it with the extracted mapping information.
	 * @param handler the handler to check, either an instance of a Spring bean name
	 */
	protected final void detectHandlerMethods(final Object handler) {
		Class<?> handlerType;
		if (handler instanceof String) {
			ApplicationContext context = getApplicationContext();
			Assert.state(context != null, "ApplicationContext is required for resolving handler bean names");
			handlerType = context.getType((String) handler);
		}
		else {
			handlerType = handler.getClass();
		}

		if (handlerType != null) {
			final Class<?> userType = ClassUtils.getUserClass(handlerType);
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> getMappingForMethod(method, userType));
			if (logger.isDebugEnabled()) {
				logger.debug(methods.size() + " message handler methods found on " + userType + ": " + methods);
			}
			methods.forEach((key, value) -> registerHandlerMethod(handler, key, value));
		}
	}

	/**
	 * Provide the mapping for a handler method.
	 * @param method the method to provide a mapping for
	 * @param handlerType the handler type, possibly a sub-type of the method's declaring class
	 * @return the mapping, or {@code null} if the method is not mapped
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * Register a handler method and its unique mapping.
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 * @throws IllegalStateException if another method was already registered
	 * under the same mapping
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		Assert.notNull(mapping, "Mapping must not be null");
		HandlerMethod newHandlerMethod = createHandlerMethod(handler, method);
		HandlerMethod oldHandlerMethod = this.handlerMethods.get(mapping);

		if (oldHandlerMethod != null && !oldHandlerMethod.equals(newHandlerMethod)) {
			throw new IllegalStateException("Ambiguous mapping found. Cannot map '" + newHandlerMethod.getBean() +
					"' bean method \n" + newHandlerMethod + "\nto " + mapping + ": There is already '" +
					oldHandlerMethod.getBean() + "' bean method\n" + oldHandlerMethod + " mapped.");
		}

		this.handlerMethods.put(mapping, newHandlerMethod);
		if (logger.isTraceEnabled()) {
			logger.trace("Mapped \"" + mapping + "\" onto " + newHandlerMethod);
		}

		for (String pattern : getDirectLookupDestinations(mapping)) {
			this.destinationLookup.add(pattern, mapping);
		}
	}

	/**
	 * Create a HandlerMethod instance from an Object handler that is either a handler
	 * instance or a String-based bean name.
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		HandlerMethod handlerMethod;
		if (handler instanceof String) {
			ApplicationContext context = getApplicationContext();
			Assert.state(context != null, "ApplicationContext is required for resolving handler bean names");
			String beanName = (String) handler;
			handlerMethod = new HandlerMethod(beanName, context.getAutowireCapableBeanFactory(), method);
		}
		else {
			handlerMethod = new HandlerMethod(handler, method);
		}
		return handlerMethod;
	}

	/**
	 * Return destinations contained in the mapping that are not patterns and are
	 * therefore suitable for direct lookups.
	 */
	protected abstract Set<String> getDirectLookupDestinations(T mapping);

	/**
	 * Return a logger to set on {@link HandlerMethodReturnValueHandlerComposite}.
	 * @since 5.1
	 */
	@Nullable
	protected Log getReturnValueHandlerLogger() {
		return null;
	}

	/**
	 * Return a logger to set on {@link InvocableHandlerMethod}.
	 * @since 5.1
	 */
	@Nullable
	protected Log getHandlerMethodLogger() {
		return null;
	}

	/**
	 * Subclasses can invoke this method to populate the MessagingAdviceBean cache
	 * (e.g. to support "global" {@code @MessageExceptionHandler}).
	 * @since 4.2
	 */
	protected void registerExceptionHandlerAdvice(
			MessagingAdviceBean bean, AbstractExceptionHandlerMethodResolver resolver) {

		this.exceptionHandlerAdviceCache.put(bean, resolver);
	}

	/**
	 * Return a map with all handler methods and their mappings.
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		return Collections.unmodifiableMap(this.handlerMethods);
	}


	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		String destination = getDestination(message);
		if (destination == null) {
			return;
		}
		String lookupDestination = getLookupDestination(destination);
		if (lookupDestination == null) {
			return;
		}

		MessageHeaderAccessor headerAccessor = MessageHeaderAccessor.getMutableAccessor(message);
		headerAccessor.setHeader(DestinationPatternsMessageCondition.LOOKUP_DESTINATION_HEADER, lookupDestination);
		headerAccessor.setLeaveMutable(true);
		message = MessageBuilder.createMessage(message.getPayload(), headerAccessor.getMessageHeaders());

		if (logger.isDebugEnabled()) {
			logger.debug("Searching methods to handle " +
					headerAccessor.getShortLogMessage(message.getPayload()) +
					", lookupDestination='" + lookupDestination + "'");
		}

		handleMessageInternal(message, lookupDestination);
		headerAccessor.setImmutable();
	}

	@Nullable
	protected abstract String getDestination(Message<?> message);

	/**
	 * Check whether the given destination (of an incoming message) matches to
	 * one of the configured destination prefixes and if so return the remaining
	 * portion of the destination after the matched prefix.
	 * <p>If there are no matching prefixes, return {@code null}.
	 * <p>If there are no destination prefixes, return the destination as is.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Nullable
	protected String getLookupDestination(@Nullable String destination) {
		if (destination == null) {
			return null;
		}
		if (CollectionUtils.isEmpty(this.destinationPrefixes)) {
			return destination;
		}
		for (int i = 0; i < this.destinationPrefixes.size(); i++) {
			String prefix = this.destinationPrefixes.get(i);
			if (destination.startsWith(prefix)) {
				return destination.substring(prefix.length());
			}
		}
		return null;
	}

	protected void handleMessageInternal(Message<?> message, String lookupDestination) {
		List<Match> matches = new ArrayList<>();

		List<T> mappingsByUrl = this.destinationLookup.get(lookupDestination);
		if (mappingsByUrl != null) {
			addMatchesToCollection(mappingsByUrl, message, matches);
		}
		if (matches.isEmpty()) {
			// No direct hits, go through all mappings
			Set<T> allMappings = this.handlerMethods.keySet();
			addMatchesToCollection(allMappings, message, matches);
		}
		if (matches.isEmpty()) {
			handleNoMatch(this.handlerMethods.keySet(), lookupDestination, message);
			return;
		}

		Comparator<Match> comparator = new MatchComparator(getMappingComparator(message));
		matches.sort(comparator);
		if (logger.isTraceEnabled()) {
			logger.trace("Found " + matches.size() + " handler methods: " + matches);
		}

		Match bestMatch = matches.get(0);
		if (matches.size() > 1) {
			Match secondBestMatch = matches.get(1);
			if (comparator.compare(bestMatch, secondBestMatch) == 0) {
				Method m1 = bestMatch.handlerMethod.getMethod();
				Method m2 = secondBestMatch.handlerMethod.getMethod();
				throw new IllegalStateException("Ambiguous handler methods mapped for destination '" +
						lookupDestination + "': {" + m1 + ", " + m2 + "}");
			}
		}

		handleMatch(bestMatch.mapping, bestMatch.handlerMethod, lookupDestination, message);
	}

	private void addMatchesToCollection(Collection<T> mappingsToCheck, Message<?> message, List<Match> matches) {
		for (T mapping : mappingsToCheck) {
			T match = getMatchingMapping(mapping, message);
			if (match != null) {
				matches.add(new Match(match, this.handlerMethods.get(mapping)));
			}
		}
	}

	/**
	 * Check if a mapping matches the current message and return a possibly
	 * new mapping with conditions relevant to the current request.
	 * @param mapping the mapping to get a match for
	 * @param message the message being handled
	 * @return the match or {@code null} if there is no match
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, Message<?> message);

	protected void handleNoMatch(Set<T> ts, String lookupDestination, Message<?> message) {
		logger.debug("No matching message handler methods.");
	}

	/**
	 * Return a comparator for sorting matching mappings.
	 * The returned comparator should sort 'better' matches higher.
	 * @param message the current Message
	 * @return the comparator, never {@code null}
	 */
	protected abstract Comparator<T> getMappingComparator(Message<?> message);

	protected void handleMatch(T mapping, HandlerMethod handlerMethod, String lookupDestination, Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + handlerMethod.getShortLogMessage());
		}
		handlerMethod = handlerMethod.createWithResolvedBean();
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		if (this.handlerMethodLogger != null) {
			invocable.setLogger(this.handlerMethodLogger);
		}
		invocable.setMessageMethodArgumentResolvers(this.argumentResolvers);
		try {
			Object returnValue = invocable.invoke(message);
			MethodParameter returnType = handlerMethod.getReturnType();
			if (void.class == returnType.getParameterType()) {
				return;
			}
			if (returnValue != null && this.returnValueHandlers.isAsyncReturnValue(returnValue, returnType)) {
				ListenableFuture<?> future = this.returnValueHandlers.toListenableFuture(returnValue, returnType);
				if (future != null) {
					future.addCallback(new ReturnValueListenableFutureCallback(invocable, message));
				}
			}
			else {
				this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
			}
		}
		catch (Exception ex) {
			processHandlerMethodException(handlerMethod, ex, message);
		}
		catch (Throwable ex) {
			Exception handlingException =
					new MessageHandlingException(message, "Unexpected handler method invocation error", ex);
			processHandlerMethodException(handlerMethod, handlingException, message);
		}
	}

	protected void processHandlerMethodException(HandlerMethod handlerMethod, Exception exception, Message<?> message) {
		InvocableHandlerMethod invocable = getExceptionHandlerMethod(handlerMethod, exception);
		if (invocable == null) {
			logger.error("Unhandled exception from message handler method", exception);
			return;
		}
		invocable.setMessageMethodArgumentResolvers(this.argumentResolvers);
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking " + invocable.getShortLogMessage());
		}
		try {
			Throwable cause = exception.getCause();
			Object returnValue = (cause != null ?
					invocable.invoke(message, exception, cause, handlerMethod) :
					invocable.invoke(message, exception, handlerMethod));
			MethodParameter returnType = invocable.getReturnType();
			if (void.class == returnType.getParameterType()) {
				return;
			}
			this.returnValueHandlers.handleReturnValue(returnValue, returnType, message);
		}
		catch (Throwable ex2) {
			logger.error("Error while processing handler method exception", ex2);
		}
	}

	/**
	 * Find an {@code @MessageExceptionHandler} method for the given exception.
	 * The default implementation searches methods in the class hierarchy of the
	 * HandlerMethod first and if not found, it continues searching for additional
	 * {@code @MessageExceptionHandler} methods among the configured
	 * {@linkplain org.springframework.messaging.handler.MessagingAdviceBean
	 * MessagingAdviceBean}, if any.
	 * @param handlerMethod the method where the exception was raised
	 * @param exception the raised exception
	 * @return a method to handle the exception, or {@code null}
	 * @since 4.2
	 */
	@Nullable
	protected InvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Searching methods to handle " + exception.getClass().getSimpleName());
		}
		Class<?> beanType = handlerMethod.getBeanType();
		AbstractExceptionHandlerMethodResolver resolver = this.exceptionHandlerCache.get(beanType);
		if (resolver == null) {
			resolver = createExceptionHandlerMethodResolverFor(beanType);
			this.exceptionHandlerCache.put(beanType, resolver);
		}
		Method method = resolver.resolveMethod(exception);
		if (method != null) {
			return new InvocableHandlerMethod(handlerMethod.getBean(), method);
		}
		for (MessagingAdviceBean advice : this.exceptionHandlerAdviceCache.keySet()) {
			if (advice.isApplicableToBeanType(beanType)) {
				resolver = this.exceptionHandlerAdviceCache.get(advice);
				method = resolver.resolveMethod(exception);
				if (method != null) {
					return new InvocableHandlerMethod(advice.resolveBean(), method);
				}
			}
		}
		return null;
	}

	protected abstract AbstractExceptionHandlerMethodResolver createExceptionHandlerMethodResolverFor(
			Class<?> beanType);


	@Override
	public String toString() {
		return getClass().getSimpleName() + "[prefixes=" + getDestinationPrefixes() + "]";
	}


	/**
	 * A thin wrapper around a matched HandlerMethod and its matched mapping for
	 * the purpose of comparing the best match with a comparator in the context
	 * of a message.
	 */
	private class Match {

		private final T mapping;

		private final HandlerMethod handlerMethod;

		public Match(T mapping, HandlerMethod handlerMethod) {
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private class ReturnValueListenableFutureCallback implements ListenableFutureCallback<Object> {

		private final InvocableHandlerMethod handlerMethod;

		private final Message<?> message;

		public ReturnValueListenableFutureCallback(InvocableHandlerMethod handlerMethod, Message<?> message) {
			this.handlerMethod = handlerMethod;
			this.message = message;
		}

		@Override
		public void onSuccess(@Nullable Object result) {
			try {
				MethodParameter returnType = this.handlerMethod.getAsyncReturnValueType(result);
				returnValueHandlers.handleReturnValue(result, returnType, this.message);
			}
			catch (Throwable ex) {
				handleFailure(ex);
			}
		}

		@Override
		public void onFailure(Throwable ex) {
			handleFailure(ex);
		}

		private void handleFailure(Throwable ex) {
			Exception cause = (ex instanceof Exception ? (Exception) ex : new IllegalStateException(ex));
			processHandlerMethodException(this.handlerMethod, cause, this.message);
		}
	}

}
