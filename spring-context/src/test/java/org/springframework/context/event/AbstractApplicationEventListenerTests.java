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

import java.io.IOException;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public abstract class AbstractApplicationEventListenerTests {

	protected ResolvableType getGenericApplicationEventType(String fieldName) {
		try {
			return ResolvableType.forField(GenericApplicationEvents.class.getField(fieldName));
		}
		catch (NoSuchFieldException e) {
			throw new IllegalStateException("No such field on Events '" + fieldName + "'");
		}
	}

	protected static class GenericApplicationEvent<T>
			extends ApplicationEvent {

		private final T payload;

		public GenericApplicationEvent(Object source, T payload) {
			super(source);
			this.payload = payload;
		}

		public T getPayload() {
			return payload;
		}

	}

	protected static class StringEvent extends GenericApplicationEvent<String> {

		public StringEvent(Object source, String payload) {
			super(source, payload);
		}
	}

	protected static class LongEvent extends GenericApplicationEvent<Long> {

		public LongEvent(Object source, Long payload) {
			super(source, payload);
		}
	}

	protected <T> GenericApplicationEvent<T> createGenericEvent(T payload) {
		return new GenericApplicationEvent<>(this, payload);
	}


	static class GenericEventListener implements ApplicationListener<GenericApplicationEvent<?>> {
		@Override
		public void onApplicationEvent(GenericApplicationEvent<?> event) {

		}
	}

	static class ObjectEventListener implements ApplicationListener<GenericApplicationEvent<Object>> {
		@Override
		public void onApplicationEvent(GenericApplicationEvent<Object> event) {

		}
	}

	static class UpperBoundEventListener
			implements ApplicationListener<GenericApplicationEvent<? extends RuntimeException>> {

		@Override
		public void onApplicationEvent(GenericApplicationEvent<? extends RuntimeException> event) {

		}
	}

	static class StringEventListener implements ApplicationListener<GenericApplicationEvent<String>> {

		@Override
		public void onApplicationEvent(GenericApplicationEvent<String> event) {

		}
	}

	static class RawApplicationListener implements ApplicationListener {
		@Override
		public void onApplicationEvent(ApplicationEvent event) {

		}
	}


	@SuppressWarnings("unused")
	static class GenericApplicationEvents {

		public GenericApplicationEvent<?> wildcardEvent;

		public GenericApplicationEvent<String> stringEvent;

		public GenericApplicationEvent<Long> longEvent;

		public GenericApplicationEvent<IllegalStateException> illegalStateExceptionEvent;

		public GenericApplicationEvent<IOException> ioExceptionEvent;

	}

}
