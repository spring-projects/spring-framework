/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link GenericApplicationListener} adapter that delegates the processing of
 * an event to an {@link EventListener} annotated method.
 *
 * <p>Delegates to {@link #processEvent(ApplicationEvent)} to give sub-classes
 * a chance to deviate from the default. Unwraps the content of a
 * {@link PayloadApplicationEvent} if necessary to allow method declaration
 * to define any arbitrary event type. If a condition is defined, it is
 * evaluated prior to invoking the underlying method.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 */
public class ApplicationListenerMethodAdapter implements GenericApplicationListener {

	protected final Log logger = LogFactory.getLog(getClass());

	private final String beanName;

	private final Method method;

	private final Class<?> targetClass;

	private final Method bridgedMethod;

	private final ResolvableType declaredEventType;

	private final AnnotatedElementKey methodKey;

	private ApplicationContext applicationContext;

	private EventExpressionEvaluator evaluator;

	private String condition;

	public ApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
		this.beanName = beanName;
		this.method = method;
		this.targetClass = targetClass;
		this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
		this.declaredEventType = resolveDeclaredEventType();
		this.methodKey = new AnnotatedElementKey(this.method, this.targetClass);
	}

	/**
	 * Initialize this instance.
	 */
	void init(ApplicationContext applicationContext, EventExpressionEvaluator evaluator) {
		this.applicationContext = applicationContext;
		this.evaluator = evaluator;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		processEvent(event);
	}

	/**
	 * Process the specified {@link ApplicationEvent}, checking if the condition
	 * match and handling non-null result, if any.
	 */
	public void processEvent(ApplicationEvent event) {
		Object[] args = resolveArguments(event);
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
	 * Resolve the method arguments to use for the specified {@link ApplicationEvent}.
	 * <p>These arguments will be used to invoke the method handled by this instance. Can
	 * return {@code null} to indicate that no suitable arguments could be resolved and
	 * therefore the method should not be invoked at all for the specified event.
	 */
	protected Object[] resolveArguments(ApplicationEvent event) {
		if (!ApplicationEvent.class.isAssignableFrom(this.declaredEventType.getRawClass())
				&& event instanceof PayloadApplicationEvent) {
			@SuppressWarnings("rawtypes")
			Object payload = ((PayloadApplicationEvent) event).getPayload();
			if (this.declaredEventType.isAssignableFrom(ResolvableType.forClass(payload.getClass()))) {
				return new Object[] {payload};
			}
		}
		else {
			return new Object[] {event};
		}
		return null;
	}

	protected void handleResult(Object result) {
		Assert.notNull(this.applicationContext, "ApplicationContext must no be null.");
		if (result.getClass().isArray()) {
			Object[] events = ObjectUtils.toObjectArray(result);
			for (Object event : events) {
				publishEvent(event);
			}
		}
		else if (result instanceof Collection<?>) {
			Collection<?> events = (Collection<?>) result;
			for (Object event : events) {
				publishEvent(event);
			}
		}
		else {
			publishEvent(result);
		}
	}

	private void publishEvent(Object event) {
		if (event != null) {
			this.applicationContext.publishEvent(event);
		}
	}

	private boolean shouldHandle(ApplicationEvent event, Object[] args) {
		if (args == null) {
			return false;
		}
		String condition = getCondition();
		if (StringUtils.hasText(condition)) {
			Assert.notNull(this.evaluator, "Evaluator must no be null.");
			EvaluationContext evaluationContext = this.evaluator.createEvaluationContext(event,
					this.targetClass, this.method, args);
			return this.evaluator.condition(condition, this.methodKey, evaluationContext);
		}
		return true;
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		if (this.declaredEventType.isAssignableFrom(eventType)) {
			return true;
		}
		else if (PayloadApplicationEvent.class.isAssignableFrom(eventType.getRawClass())) {
			ResolvableType payloadType = eventType.as(PayloadApplicationEvent.class).getGeneric();
			return eventType.hasUnresolvableGenerics() || this.declaredEventType.isAssignableFrom(payloadType);
		}
		return false;
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	@Override
	public int getOrder() {
		Order order = getMethodAnnotation(Order.class);
		return (order != null ? order.value() : 0);
	}

	protected <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
		return AnnotationUtils.findAnnotation(this.method, annotationType);
	}

	/**
	 * Invoke the event listener method with the given argument values.
	 */
	protected Object doInvoke(Object... args) {
		Object bean = getTargetBean();
		ReflectionUtils.makeAccessible(this.bridgedMethod);
		try {
			return this.bridgedMethod.invoke(bean, args);
		}
		catch (IllegalArgumentException ex) {
			assertTargetBean(this.bridgedMethod, bean, args);
			throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(getInvocationErrorMessage(bean, ex.getMessage(), args), ex);
		}
		catch (InvocationTargetException ex) {
			// Throw underlying exception
			Throwable targetException = ex.getTargetException();
			if (targetException instanceof RuntimeException) {
				throw (RuntimeException) targetException;
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
		Assert.notNull(this.applicationContext, "ApplicationContext must no be null.");
		return this.applicationContext.getBean(this.beanName);
	}

	/**
	 * Return the condition to use.
	 * <p>Matches the {@code condition} attribute of the {@link EventListener}
	 * annotation or any matching attribute on a composed annotation that
	 * is meta-annotated with {@code @EventListener}.
	 */
	protected String getCondition() {
		if (this.condition == null) {
			AnnotationAttributes annotationAttributes = AnnotatedElementUtils
					.findAnnotationAttributes(this.method, EventListener.class);
			if (annotationAttributes != null) {
				String value = annotationAttributes.getString("condition");
				this.condition = (value != null ? value : "");
			}
		}
		return this.condition;
	}

	/**
	 * Add additional details such as the bean type and method signature to
	 * the given error message.
	 * @param message error message to append the HandlerMethod details to
	 */
	protected String getDetailedErrorMessage(Object bean, String message) {
		StringBuilder sb = new StringBuilder(message).append("\n");
		sb.append("HandlerMethod details: \n");
		sb.append("Bean [").append(bean.getClass().getName()).append("]\n");
		sb.append("Method [").append(this.bridgedMethod.toGenericString()).append("]\n");
		return sb.toString();
	}

	/**
	 * Assert that the target bean class is an instance of the class where the given
	 * method is declared. In some cases the actual bean instance at event-
	 * processing time may be a JDK dynamic proxy (lazy initialization, prototype
	 * beans, and others). Event listener beans that require proxying should prefer
	 * class-based proxy mechanisms.
	 */
	private void assertTargetBean(Method method, Object targetBean, Object[] args) {
		Class<?> methodDeclaringClass = method.getDeclaringClass();
		Class<?> targetBeanClass = targetBean.getClass();
		if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
			String msg = "The event listener method class '" + methodDeclaringClass.getName() +
					"' is not an instance of the actual bean instance '" +
					targetBeanClass.getName() + "'. If the bean requires proxying " +
					"(e.g. due to @Transactional), please use class-based proxying.";
			throw new IllegalStateException(getInvocationErrorMessage(targetBean, msg, args));
		}
	}

	private String getInvocationErrorMessage(Object bean, String message, Object[] resolvedArgs) {
		StringBuilder sb = new StringBuilder(getDetailedErrorMessage(bean, message));
		sb.append("Resolved arguments: \n");
		for (int i = 0; i < resolvedArgs.length; i++) {
			sb.append("[").append(i).append("] ");
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


	private ResolvableType resolveDeclaredEventType() {
		int count = this.method.getParameterTypes().length;
		if (count != 1) {
			throw new IllegalStateException("Only one parameter is allowed " +
					"for event listener method: " + method);
		}
		return ResolvableType.forMethodParameter(this.method, 0);
	}

	@Override
	public String toString() {
		return this.method.toGenericString();
	}

}
