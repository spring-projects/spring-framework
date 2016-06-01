/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.core.ResolvableTypeProvider;

/**
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public abstract class AbstractApplicationEventListenerTests {

	protected ResolvableType getGenericApplicationEventType(String fieldName) {
		try {
			return ResolvableType.forField(TestEvents.class.getField(fieldName));
		}
		catch (NoSuchFieldException e) {
			throw new IllegalStateException("No such field on Events '" + fieldName + "'");
		}
	}

	protected static class GenericTestEvent<T>
			extends ApplicationEvent {

		private final T payload;

		public GenericTestEvent(Object source, T payload) {
			super(source);
			this.payload = payload;
		}

		public T getPayload() {
			return this.payload;
		}

	}

	protected static class SmartGenericTestEvent<T>
			extends GenericTestEvent<T> implements ResolvableTypeProvider {

		private final ResolvableType resolvableType;

		public SmartGenericTestEvent(Object source, T payload) {
			super(source, payload);
			this.resolvableType = ResolvableType.forClassWithGenerics(
					getClass(), payload.getClass());
		}

		@Override
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}
	}

	protected static class StringEvent extends GenericTestEvent<String> {

		public StringEvent(Object source, String payload) {
			super(source, payload);
		}
	}

	protected static class LongEvent extends GenericTestEvent<Long> {

		public LongEvent(Object source, Long payload) {
			super(source, payload);
		}
	}

	protected <T> GenericTestEvent<T> createGenericTestEvent(T payload) {
		return new GenericTestEvent<>(this, payload);
	}


	static class GenericEventListener implements ApplicationListener<GenericTestEvent<?>> {
		@Override
		public void onApplicationEvent(GenericTestEvent<?> event) {
		}
	}

	static class ObjectEventListener implements ApplicationListener<GenericTestEvent<Object>> {
		@Override
		public void onApplicationEvent(GenericTestEvent<Object> event) {
		}
	}

	static class UpperBoundEventListener
			implements ApplicationListener<GenericTestEvent<? extends RuntimeException>> {

		@Override
		public void onApplicationEvent(GenericTestEvent<? extends RuntimeException> event) {
		}
	}

	static class StringEventListener implements ApplicationListener<GenericTestEvent<String>> {

		@Override
		public void onApplicationEvent(GenericTestEvent<String> event) {
		}
	}

	@SuppressWarnings("rawtypes")
	static class RawApplicationListener implements ApplicationListener {
		@Override
		public void onApplicationEvent(ApplicationEvent event) {
		}
	}

	static class TestEvents {

		public ApplicationEvent applicationEvent;

		public GenericTestEvent<?> wildcardEvent;

		public GenericTestEvent<String> stringEvent;

		public GenericTestEvent<Long> longEvent;

		public GenericTestEvent<IllegalStateException> illegalStateExceptionEvent;

		public GenericTestEvent<IOException> ioExceptionEvent;

	}

}
