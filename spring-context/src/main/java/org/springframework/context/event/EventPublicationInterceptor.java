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

import java.lang.reflect.Constructor;
import java.util.function.Function;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Assert;

/**
 * {@link MethodInterceptor Interceptor} that publishes an {@code ApplicationEvent}
 * to all {@code ApplicationListeners} registered with an {@code ApplicationEventPublisher}.
 * after each <i>successful</i> method invocation.
 *
 * <p>Note that this interceptor is capable of publishing a custom event after each
 * <i>successful</i> method invocation, configured via the
 * {@link #setApplicationEventClass "applicationEventClass"} property. As of 7.0.3,
 * you can configure a {@link #setApplicationEventFactory factory function} instead.
 *
 * <p>As of 7.0.3, this interceptor publishes a {@link MethodFailureEvent} for
 * every exception encountered from a method invocation. This can be conveniently
 * tracked via an {@code ApplicationListener<MethodFailureEvent>} class or an
 * {@code @EventListener(MethodFailureEvent.class)} method.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see #setApplicationEventClass
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.ApplicationEventPublisher
 * @see org.springframework.context.ApplicationContext
 */
public class EventPublicationInterceptor
		implements MethodInterceptor, ApplicationEventPublisherAware, InitializingBean {

	private @Nullable Function<MethodInvocation, ? extends ApplicationEvent> applicationEventFactory;

	private @Nullable ApplicationEventPublisher applicationEventPublisher;


	/**
	 * Set the application event class to publish.
	 * <p>The event class <b>must</b> have a constructor with a single
	 * {@code Object} argument for the event source. The interceptor
	 * will pass in the invoked object.
	 * @throws IllegalArgumentException if the supplied {@code Class} is
	 * {@code null} or if it is not an {@code ApplicationEvent} subclass or
	 * if it does not expose a constructor that takes a single {@code Object} argument
	 * @see #setApplicationEventFactory
	 */
	public void setApplicationEventClass(Class<? extends ApplicationEvent> applicationEventClass) {
		if (ApplicationEvent.class == applicationEventClass ||
				!ApplicationEvent.class.isAssignableFrom(applicationEventClass)) {
			throw new IllegalArgumentException("'applicationEventClass' needs to extend ApplicationEvent");
		}
		try {
			Constructor<? extends ApplicationEvent> ctor = applicationEventClass.getConstructor(Object.class);
			this.applicationEventFactory = (invocation -> BeanUtils.instantiateClass(ctor, invocation.getThis()));
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("ApplicationEvent class [" +
					applicationEventClass.getName() + "] does not have the required Object constructor: " + ex);
		}
	}

	/**
	 * Specify a factory function for {@link ApplicationEvent} instances built from a
	 * {@link MethodInvocation}, representing a <i>successful</i> method invocation.
	 * @since 7.0.3
	 * @see #setApplicationEventClass
	 */
	public void setApplicationEventFactory(Function<MethodInvocation, ? extends ApplicationEvent> factoryFunction) {
		this.applicationEventFactory = factoryFunction;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.applicationEventPublisher == null) {
			throw new IllegalArgumentException("Property 'applicationEventPublisher' is required");
		}
	}


	@Override
	public @Nullable Object invoke(MethodInvocation invocation) throws Throwable {
		Assert.state(this.applicationEventPublisher != null, "No ApplicationEventPublisher available");

		Object retVal;
		try {
			retVal = invocation.proceed();
		}
		catch (Throwable ex) {
			this.applicationEventPublisher.publishEvent(new MethodFailureEvent(invocation, ex));
			throw ex;
		}

		if (this.applicationEventFactory != null) {
			this.applicationEventPublisher.publishEvent(this.applicationEventFactory.apply(invocation));
		}
		return retVal;
	}

}
