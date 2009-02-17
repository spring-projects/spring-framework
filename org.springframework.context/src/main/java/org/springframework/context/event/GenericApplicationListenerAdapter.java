/*
 * Copyright 2002-2009 the original author or authors.
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * {@link SmartApplicationListener} adapter that determines supported event types
 * through introspecting the generically declared type of the target listener.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.context.ApplicationListener#onApplicationEvent
 */
public class GenericApplicationListenerAdapter implements SmartApplicationListener {

	private final ApplicationListener delegate;


	/**
	 * Create a new GenericApplicationListener for the given delegate.
	 * @param delegate the delegate listener to be invoked
	 */
	public GenericApplicationListenerAdapter(ApplicationListener delegate) {
		Assert.notNull(delegate, "Delegate listener must not be null");
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	public void onApplicationEvent(ApplicationEvent event) {
		this.delegate.onApplicationEvent(event);
	}

	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return getGenericEventType(this.delegate.getClass()).isAssignableFrom(eventType);
	}

	public boolean supportsSourceType(Class<?> sourceType) {
		return true;
	}

	public int getOrder() {
		return (this.delegate instanceof Ordered ? ((Ordered) this.delegate).getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	@SuppressWarnings("unchecked")
	private Class<? extends ApplicationEvent> getGenericEventType(Class<? extends ApplicationListener> currentClass) {
		Class classToIntrospect = currentClass;
		while (classToIntrospect != null) {
			Type[] ifcs = classToIntrospect.getGenericInterfaces();
			for (Type ifc : ifcs) {
				if (ifc instanceof ParameterizedType) {
					ParameterizedType paramIfc = (ParameterizedType) ifc;
					Type rawType = paramIfc.getRawType();
					if (ApplicationListener.class.equals(rawType)) {
						Type arg = paramIfc.getActualTypeArguments()[0];
						if (arg instanceof TypeVariable) {
							arg = GenericTypeResolver.resolveTypeVariable((TypeVariable) arg, this.delegate.getClass());
						}
						if (arg instanceof Class) {
							return (Class) arg;
						}
					}
					else if (ApplicationListener.class.isAssignableFrom((Class) rawType)) {
						return getGenericEventType((Class) rawType);
					}
				}
				else if (ApplicationListener.class.isAssignableFrom((Class) ifc)) {
					return getGenericEventType((Class) ifc);
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return ApplicationEvent.class;
	}

}
