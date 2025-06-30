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

package org.springframework.context.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletionStage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.CoroutinesUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.core.Ordered;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link GenericApplicationListener} adapter that delegates the processing of
 * an event to an {@link EventListener} annotated method.
 *
 * <p>Delegates to {@link #processEvent(ApplicationEvent)} to give subclasses
 * a chance to deviate from the default. Unwraps the content of a
 * {@link PayloadApplicationEvent} if necessary to allow a method declaration
 * to define any arbitrary event type. If a condition is defined, it is
 * evaluated prior to invoking the underlying method.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Yanming Zhou
 * @since 4.2
 */
public class ApplicationListenerMethodAdapter implements GenericApplicationListener {

	private static final boolean reactiveStreamsPresent = ClassUtils.isPresent(
			"org.reactivestreams.Publisher", ApplicationListenerMethodAdapter.class.getClassLoader());


	protected final Log logger = LogFactory.getLog(getClass());

	private final String beanName;

	private final Method method;

	private final Method targetMethod;

	private final AnnotatedElementKey methodKey;

	private final List<ResolvableType> declaredEventTypes;

	private final @Nullable String condition;

	private final boolean defaultExecution;

	private final int order;

	private volatile @Nullable String listenerId;

	private @Nullable ApplicationContext applicationContext;

	private @Nullable EventExpressionEvaluator evaluator;


	/**
	 * Construct a new ApplicationListenerMethodAdapter.
	 * @param beanName the name of the bean to invoke the listener method on
	 * @param targetClass the target class that the method is declared on
	 * @param method the listener method to invoke
	 */
	public ApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
		this.beanName = beanName;
		this.method = BridgeMethodResolver.findBridgedMethod(method);
		this.targetMethod = (!Proxy.isProxyClass(targetClass) ?
				AopUtils.getMostSpecificMethod(method, targetClass) : this.method);
		this.methodKey = new AnnotatedElementKey(this.targetMethod, targetClass);

		EventListener ann = AnnotatedElementUtils.findMergedAnnotation(this.targetMethod, EventListener.class);
		this.declaredEventTypes = resolveDeclaredEventTypes(method, ann);
		this.condition = (ann != null ? ann.condition() : null);
		this.defaultExecution = (ann == null || ann.defaultExecution());
		this.order = resolveOrder(this.targetMethod);
		String id = (ann != null ? ann.id() : "");
		this.listenerId = (!id.isEmpty() ? id : null);
	}

	private static List<ResolvableType> resolveDeclaredEventTypes(Method method, @Nullable EventListener ann) {
		int count = (KotlinDetector.isSuspendingFunction(method) ? method.getParameterCount() - 1 : method.getParameterCount());
		if (count > 1) {
			throw new IllegalStateException(
					"Maximum one parameter is allowed for event listener method: " + method);
		}

		if (ann != null) {
			Class<?>[] classes = ann.classes();
			if (classes.length > 0) {
				List<ResolvableType> types = new ArrayList<>(classes.length);
				for (Class<?> eventType : classes) {
					types.add(ResolvableType.forClass(eventType));
				}
				return types;
			}
		}

		if (count == 0) {
			throw new IllegalStateException(
					"Event parameter is mandatory for event listener method: " + method);
		}
		return Collections.singletonList(ResolvableType.forMethodParameter(method, 0));
	}

	private static int resolveOrder(Method method) {
		Order ann = AnnotatedElementUtils.findMergedAnnotation(method, Order.class);
		return (ann != null ? ann.value() : Ordered.LOWEST_PRECEDENCE);
	}


	/**
	 * Initialize this instance.
	 */
	void init(ApplicationContext applicationContext, @Nullable EventExpressionEvaluator evaluator) {
		this.applicationContext = applicationContext;
		this.evaluator = evaluator;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (isDefaultExecution()) {
			processEvent(event);
		}
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		for (ResolvableType declaredEventType : this.declaredEventTypes) {
			if (eventType.hasUnresolvableGenerics() ?
					declaredEventType.toClass().isAssignableFrom(eventType.toClass()) :
					declaredEventType.isAssignableFrom(eventType)) {
				return true;
			}
			if (PayloadApplicationEvent.class.isAssignableFrom(eventType.toClass())) {
				ResolvableType payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
				if (declaredEventType.isAssignableFrom(payloadType)) {
					return true;
				}
				if (payloadType.resolve() == null) {
					// Always accept such event when the type is erased
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return true;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public String getListenerId() {
		String id = this.listenerId;
		if (id == null) {
			id = getDefaultListenerId();
			this.listenerId = id;
		}
		return id;
	}

	/**
	 * Determine the default id for the target listener, to be applied in case of
	 * no {@link EventListener#id() annotation-specified id value}.
	 * <p>The default implementation builds a method name with parameter types.
	 * @since 5.3.5
	 * @see #getListenerId()
	 */
	protected String getDefaultListenerId() {
		Method method = getTargetMethod();
		StringJoiner sj = new StringJoiner(",", "(", ")");
		for (Class<?> paramType : method.getParameterTypes()) {
			sj.add(paramType.getName());
		}
		return ClassUtils.getQualifiedMethodName(method) + sj;
	}

	/**
	 * Return whether default execution is applicable for the target listener.
	 * @since 6.2
	 * @see #onApplicationEvent
	 * @see EventListener#defaultExecution()
	 */
	protected boolean isDefaultExecution() {
		return this.defaultExecution;
	}


	/**
	 * Process the specified {@link ApplicationEvent}, checking if the condition
	 * matches and handling a non-null result, if any.
	 * @param event the event to process through the listener method
	 */
	public void processEvent(ApplicationEvent event) {
		@Nullable Object[] args = resolveArguments(event);
		if (shouldHandle(event, args)) {
			Object result = doInvoke(args);
			if (result != null) {
				handleResult(result);
			}
			else {
				logger.trace("No result object given - no result to handle");
			}
		}
	}

	/**
	 * Determine whether the listener method would actually handle the given
	 * event, checking if the condition matches.
	 * @param event the event to process through the listener method
	 * @since 6.1
	 */
	public boolean shouldHandle(ApplicationEvent event) {
		return shouldHandle(event, resolveArguments(event));
	}

	@Contract("_, null -> false")
	private boolean shouldHandle(ApplicationEvent event, @Nullable Object @Nullable [] args) {
		if (args == null) {
			return false;
		}
		String condition = getCondition();
		if (StringUtils.hasText(condition)) {
			Assert.notNull(this.evaluator, "EventExpressionEvaluator must not be null");
			return this.evaluator.condition(
					condition, event, this.targetMethod, this.methodKey, args);
		}
		return true;
	}

	/**
	 * Resolve the method arguments to use for the specified {@link ApplicationEvent}.
	 * <p>These arguments will be used to invoke the method handled by this instance.
	 * Can return {@code null} to indicate that no suitable arguments could be resolved
	 * and therefore the method should not be invoked at all for the specified event.
	 */
	protected @Nullable Object @Nullable [] resolveArguments(ApplicationEvent event) {
		ResolvableType declaredEventType = getResolvableType(event);
		if (declaredEventType == null) {
			return null;
		}
		if (this.method.getParameterCount() == 0) {
			return new Object[0];
		}
		Class<?> declaredEventClass = declaredEventType.toClass();
		if (!ApplicationEvent.class.isAssignableFrom(declaredEventClass) &&
				event instanceof PayloadApplicationEvent<?> payloadEvent) {
			Object payload = payloadEvent.getPayload();
			if (declaredEventClass.isInstance(payload)) {
				return new Object[] {payload};
			}
		}
		return new Object[] {event};
	}

	protected void handleResult(Object result) {
		if (reactiveStreamsPresent && new ReactiveResultHandler().subscribeToPublisher(result)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Adapted to reactive result: " + result);
			}
		}
		else if (result instanceof CompletionStage<?> completionStage) {
			completionStage.whenComplete((event, ex) -> {
				if (ex != null) {
					handleAsyncError(ex);
				}
				else if (event != null) {
					publishEvents(event);
				}
			});
		}
		else {
			publishEvents(result);
		}
	}

	private void publishEvents(@Nullable Object result) {
		if (result != null && result.getClass().isArray()) {
			Object[] events = ObjectUtils.toObjectArray(result);
			for (Object event : events) {
				publishEvent(event);
			}
		}
		else if (result instanceof Collection<?> events) {
			for (Object event : events) {
				publishEvent(event);
			}
		}
		else {
			publishEvent(result);
		}
	}

	private void publishEvent(@Nullable Object event) {
		if (event != null) {
			Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
			this.applicationContext.publishEvent(event);
		}
	}

	protected void handleAsyncError(Throwable t) {
		logger.error("Unexpected error occurred in asynchronous listener", t);
	}

	/**
	 * Invoke the event listener method with the given argument values.
	 */
	protected @Nullable Object doInvoke(@Nullable Object... args) {
		Object bean = getTargetBean();
		// Detect package-protected NullBean instance through equals(null) check
		if (bean.equals(null)) {
			return null;
		}

		ReflectionUtils.makeAccessible(this.method);
		try {
			if (KotlinDetector.isSuspendingFunction(this.method)) {
				return CoroutinesUtils.invokeSuspendingFunction(this.method, bean, args);
			}
			return this.method.invoke(bean, args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(this.method, bean, args);
			throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
		}
		catch (InvocationTargetException ex) {
			// Throw underlying exception
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			else {
				String msg = getInvocationErrorMessage(bean, "Failed to invoke event listener method", args);
				throw new UndeclaredThrowableException(targetException, msg);
			}
		}
	}

	/**
	 * Return the target bean instance to use.
	 */
	protected Object getTargetBean() {
		Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
		return this.applicationContext.getBean(this.beanName);
	}

	/**
	 * Return the target listener method.
	 * @since 5.3
	 */
	protected Method getTargetMethod() {
		return this.targetMethod;
	}

	/**
	 * Return the condition to use.
	 * <p>Matches the {@code condition} attribute of the {@link EventListener}
	 * annotation or any matching attribute on a composed annotation that
	 * is meta-annotated with {@code @EventListener}.
	 */
	protected @Nullable String getCondition() {
		return this.condition;
	}

	/**
	 * Add additional details such as the bean type and method signature to
	 * the given error message.
	 * @param message error message to append the HandlerMethod details to
	 */
	protected String getDetailedErrorMessage(Object bean, @Nullable String message) {
		StringBuilder sb = (StringUtils.hasLength(message) ? new StringBuilder(message).append('\n') : new StringBuilder());
		sb.append("HandlerMethod details: \n");
		sb.append("Bean [").append(bean.getClass().getName()).append("]\n");
		sb.append("Method [").append(this.method.toGenericString()).append("]\n");
		return sb.toString();
	}

	/**
	 * Assert that the target bean class is an instance of the class where the given
	 * method is declared. In some cases the actual bean instance at event-
	 * processing time may be a JDK dynamic proxy (lazy initialization, prototype
	 * beans, and others). Event listener beans that require proxying should prefer
	 * class-based proxy mechanisms.
	 */
	private void assertTargetBean(Method method, Object targetBean, @Nullable Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String msg = "The event listener method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual bean class '" +
					targetBeanClass.getName() + "'. If the bean requires proxying " +
					"(for example, due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(targetBean, msg, args));
		}
	}

	private String getInvocationErrorMessage(Object bean, @Nullable String message, @Nullable Object [] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(bean, message));
		sb.append("Resolved arguments: \n");
		for (int i = 0; i < resolvedArgs.length; i++) {
			sb.append('[').append(i).append("] ");
			if (resolvedArgs[i] == null) {
				sb.append("[null] \n");
			}
			else {
				sb.append("[type=").append(resolvedArgs[i].getClass().getName()).append("] ");
				sb.append("[value=").append(resolvedArgs[i]).append("]\n");
			}
		}
		return sb.toString();
	}

	private @Nullable ResolvableType getResolvableType(ApplicationEvent event) {
		ResolvableType payloadType = null;
		if (event instanceof PayloadApplicationEvent<?> payloadEvent) {
			ResolvableType eventType = payloadEvent.getResolvableType();
			if (eventType != null) {
				payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
			}
		}
		for (ResolvableType declaredEventType : this.declaredEventTypes) {
			Class<?> eventClass = declaredEventType.toClass();
			if (!ApplicationEvent.class.isAssignableFrom(eventClass) &&
					payloadType != null && declaredEventType.isAssignableFrom(payloadType)) {
				return declaredEventType;
			}
			if (eventClass.isInstance(event)) {
				return declaredEventType;
			}
		}
		return null;
	}


	@Override
	public String toString() {
		return this.method.toGenericString();
	}


	/**
	 * Inner class to avoid a hard dependency on the Reactive Streams API at runtime.
	 */
	private class ReactiveResultHandler {

		public boolean subscribeToPublisher(Object result) {
			ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(result.getClass());
			if (adapter != null) {
				adapter.toPublisher(result).subscribe(new EventPublicationSubscriber());
				return true;
			}
			return false;
		}
	}


	/**
	 * Reactive Streams Subscriber for publishing follow-up events.
	 */
	private class EventPublicationSubscriber implements Subscriber<Object> {

		@Override
		public void onSubscribe(Subscription s) {
			s.request(Integer.MAX_VALUE);
		}

		@Override
		public void onNext(Object o) {
			publishEvents(o);
		}

		@Override
		public void onError(Throwable t) {
			handleAsyncError(t);
		}

		@Override
		public void onComplete() {
		}
	}

}
