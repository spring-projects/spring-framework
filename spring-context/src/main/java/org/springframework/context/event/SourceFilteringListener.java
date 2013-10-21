/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * {@link org.springframework.context.ApplicationListener} decorator that filters
 * events from a specified event source, invoking its delegate listener for
 * matching {@link org.springframework.context.ApplicationEvent} objects only.
 *
 * <p>Can also be used as base class, overriding the {@link #onApplicationEventInternal}
 * method instead of specifying a delegate listener.
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 */
public class SourceFilteringListener implements SmartApplicationListener {

	private final Object source;

	private SmartApplicationListener delegate;


	/**
	 * Create a SourceFilteringListener for the given event source.
	 * @param source the event source that this listener filters for,
	 * only processing events from this source
	 * @param delegate the delegate listener to invoke with event
	 * from the specified source
	 */
	public SourceFilteringListener(Object source, ApplicationListener delegate) {
		this.source = source;
		this.delegate = (delegate instanceof SmartApplicationListener ?
				(SmartApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
	}

	/**
	 * Create a SourceFilteringListener for the given event source,
	 * expecting subclasses to override the {@link #onApplicationEventInternal}
	 * method (instead of specifying a delegate listener).
	 * @param source the event source that this listener filters for,
	 * only processing events from this source
	 */
	protected SourceFilteringListener(Object source) {
		this.source = source;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event.getSource() == this.source) {
			onApplicationEventInternal(event);
		}
	}

	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return (this.delegate == null || this.delegate.supportsEventType(eventType));
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return (sourceType != null && sourceType.isInstance(this.source));
	}

	@Override
	public int getOrder() {
		return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
	}


	/**
	 * Actually process the event, after having filtered according to the
	 * desired event source already.
	 * <p>The default implementation invokes the specified delegate, if any.
	 * @param event the event to process (matching the specified source)
	 */
	protected void onApplicationEventInternal(ApplicationEvent event) {
		if (this.delegate == null) {
			throw new IllegalStateException(
					"Must specify a delegate object or override the onApplicationEventInternal method");
		}
		this.delegate.onApplicationEvent(event);
	}

}
