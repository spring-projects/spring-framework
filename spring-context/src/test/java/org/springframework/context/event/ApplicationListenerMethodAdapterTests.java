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
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Stephane Nicoll
 */
public class ApplicationListenerMethodAdapterTests extends AbstractApplicationEventListenerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final SampleEvents sampleEvents = spy(new SampleEvents());

	private final ApplicationContext context = mock(ApplicationContext.class);

	@Test
	public void rawListener() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleRaw", ApplicationEvent.class);
		supportsEventType(true, method, getGenericApplicationEventType("applicationEvent"));
	}

	@Test
	public void rawListenerWithGenericEvent() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleRaw", ApplicationEvent.class);
		supportsEventType(true, method, getGenericApplicationEventType("stringEvent"));
	}

	@Test
	public void genericListener() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericString", GenericTestEvent.class);
		supportsEventType(true, method, getGenericApplicationEventType("stringEvent"));
	}

	@Test
	public void genericListenerWrongParameterizedType() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericString", GenericTestEvent.class);
		supportsEventType(false, method, getGenericApplicationEventType("longEvent"));
	}

	@Test
	public void listenerWithPayloadAndGenericInformation() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		supportsEventType(true, method, createGenericEventType(String.class));
	}

	@Test
	public void listenerWithInvalidPayloadAndGenericInformation() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		supportsEventType(false, method, createGenericEventType(Integer.class));
	}

	@Test
	public void listenerWithPayloadTypeErasure() { // Always accept such event when the type is unknown
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		supportsEventType(true, method, ResolvableType.forClass(PayloadApplicationEvent.class));
	}

	@Test
	public void listenerWithSubTypeSeveralGenerics() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		supportsEventType(true, method, ResolvableType.forClass(PayloadTestEvent.class));
	}

	@Test
	public void listenerWithSubTypeSeveralGenericsResolved() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		supportsEventType(true, method, ResolvableType.forClass(PayloadStringTestEvent.class));
	}

	@Test
	public void listenerWithAnnotationValue() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringAnnotationValue");
		supportsEventType(true, method, createGenericEventType(String.class));
	}

	@Test
	public void listenerWithAnnotationClasses() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringAnnotationClasses");
		supportsEventType(true, method, createGenericEventType(String.class));
	}

	@Test
	public void listenerWithAnnotationValueAndParameter() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringAnnotationValueAndParameter", String.class);
		supportsEventType(true, method, createGenericEventType(String.class));
	}

	@Test
	public void listenerWithSeveralTypes() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringOrInteger");
		supportsEventType(true, method, createGenericEventType(String.class));
		supportsEventType(true, method, createGenericEventType(Integer.class));
		supportsEventType(false, method, createGenericEventType(Double.class));
	}

	@Test
	public void listenerWithTooManyParameters() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"tooManyParameters", String.class, String.class);

		this.thrown.expect(IllegalStateException.class);
		createTestInstance(method);
	}

	@Test
	public void listenerWithNoParameter() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"noParameter");

		this.thrown.expect(IllegalStateException.class);
		createTestInstance(method);
	}

	@Test
	public void listenerWithMoreThanOneParameter() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"moreThanOneParameter", String.class, Integer.class);

		this.thrown.expect(IllegalStateException.class);
		createTestInstance(method);
	}

	@Test
	public void defaultOrder() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericString", GenericTestEvent.class);
		ApplicationListenerMethodAdapter adapter = createTestInstance(method);
		assertEquals(0, adapter.getOrder());
	}

	@Test
	public void specifiedOrder() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleRaw", ApplicationEvent.class);
		ApplicationListenerMethodAdapter adapter = createTestInstance(method);
		assertEquals(42, adapter.getOrder());
	}

	@Test
	public void invokeListener() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericString", GenericTestEvent.class);
		GenericTestEvent<String> event = createGenericTestEvent("test");
		invokeListener(method, event);
		verify(this.sampleEvents, times(1)).handleGenericString(event);
	}

	@Test
	public void invokeListenerWithGenericEvent() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericString", GenericTestEvent.class);
		GenericTestEvent<String> event = new SmartGenericTestEvent<>(this, "test");
		invokeListener(method, event);
		verify(this.sampleEvents, times(1)).handleGenericString(event);
	}

	@Test
	public void invokeListenerWithGenericPayload() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericStringPayload", EntityWrapper.class);
		EntityWrapper<String> payload = new EntityWrapper<>("test");
		invokeListener(method, new PayloadApplicationEvent<>(this, payload));
		verify(this.sampleEvents, times(1)).handleGenericStringPayload(payload);
	}

	@Test
	public void invokeListenerWithWrongGenericPayload() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericStringPayload", EntityWrapper.class);
		EntityWrapper<Integer> payload = new EntityWrapper<>(123);
		invokeListener(method, new PayloadApplicationEvent<>(this, payload));
		verify(this.sampleEvents, times(0)).handleGenericStringPayload(any());
	}

	@Test
	public void invokeListenerWithAnyGenericPayload() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericAnyPayload", EntityWrapper.class);
		EntityWrapper<String> payload = new EntityWrapper<>("test");
		invokeListener(method, new PayloadApplicationEvent<>(this, payload));
		verify(this.sampleEvents, times(1)).handleGenericAnyPayload(payload);
	}

	@Test
	public void invokeListenerRuntimeException() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"generateRuntimeException", GenericTestEvent.class);
		GenericTestEvent<String> event = createGenericTestEvent("fail");

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Test exception");
		this.thrown.expectCause(is(isNull(Throwable.class)));
		invokeListener(method, event);
	}

	@Test
	public void invokeListenerCheckedException() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"generateCheckedException", GenericTestEvent.class);
		GenericTestEvent<String> event = createGenericTestEvent("fail");

		this.thrown.expect(UndeclaredThrowableException.class);
		this.thrown.expectCause(is(instanceOf(IOException.class)));
		invokeListener(method, event);
	}

	@Test
	public void invokeListenerInvalidProxy() {
		Object target = new InvalidProxyTestBean();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(target);
		proxyFactory.addInterface(SimpleService.class);
		Object bean = proxyFactory.getProxy(getClass().getClassLoader());

		Method method = ReflectionUtils.findMethod(InvalidProxyTestBean.class, "handleIt2", ApplicationEvent.class);
		StaticApplicationListenerMethodAdapter listener =
				new StaticApplicationListenerMethodAdapter(method, bean);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("handleIt2");
		listener.onApplicationEvent(createGenericTestEvent("test"));
	}

	@Test
	public void invokeListenerWithPayload() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
		invokeListener(method, event);
		verify(this.sampleEvents, times(1)).handleString("test");
	}

	@Test
	public void invokeListenerWithPayloadWrongType() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleString", String.class);
		PayloadApplicationEvent<Long> event = new PayloadApplicationEvent<>(this, 123L);
		invokeListener(method, event);
		verify(this.sampleEvents, never()).handleString(anyString());
	}

	@Test
	public void invokeListenerWithAnnotationValue() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringAnnotationClasses");
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
		invokeListener(method, event);
		verify(this.sampleEvents, times(1)).handleStringAnnotationClasses();
	}

	@Test
	public void invokeListenerWithAnnotationValueAndParameter() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringAnnotationValueAndParameter", String.class);
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
		invokeListener(method, event);
		verify(this.sampleEvents, times(1)).handleStringAnnotationValueAndParameter("test");
	}

	@Test
	public void invokeListenerWithSeveralTypes() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleStringOrInteger");
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "test");
		invokeListener(method, event);
		verify(this.sampleEvents, times(1)).handleStringOrInteger();
		PayloadApplicationEvent<Integer> event2 = new PayloadApplicationEvent<>(this, 123);
		invokeListener(method, event2);
		verify(this.sampleEvents, times(2)).handleStringOrInteger();
		PayloadApplicationEvent<Double> event3 = new PayloadApplicationEvent<>(this, 23.2);
		invokeListener(method, event3);
		verify(this.sampleEvents, times(2)).handleStringOrInteger();
	}


	@Test
	public void beanInstanceRetrievedAtEveryInvocation() {
		Method method = ReflectionUtils.findMethod(SampleEvents.class,
				"handleGenericString", GenericTestEvent.class);
		when(this.context.getBean("testBean")).thenReturn(this.sampleEvents);
		ApplicationListenerMethodAdapter listener = new ApplicationListenerMethodAdapter(
				"testBean", GenericTestEvent.class, method);
		listener.init(this.context, new EventExpressionEvaluator());
		GenericTestEvent<String> event = createGenericTestEvent("test");


		listener.onApplicationEvent(event);
		verify(this.sampleEvents, times(1)).handleGenericString(event);
		verify(this.context, times(1)).getBean("testBean");

		listener.onApplicationEvent(event);
		verify(this.sampleEvents, times(2)).handleGenericString(event);
		verify(this.context, times(2)).getBean("testBean");
	}

	private void supportsEventType(boolean match, Method method, ResolvableType eventType) {
		ApplicationListenerMethodAdapter adapter = createTestInstance(method);
		assertEquals("Wrong match for event '" + eventType + "' on " + method,
				match, adapter.supportsEventType(eventType));
	}

	private void invokeListener(Method method, ApplicationEvent event) {
		ApplicationListenerMethodAdapter adapter = createTestInstance(method);
		adapter.onApplicationEvent(event);
	}

	private ApplicationListenerMethodAdapter createTestInstance(Method method) {
		return new StaticApplicationListenerMethodAdapter(method, this.sampleEvents);
	}

	private ResolvableType createGenericEventType(Class<?> payloadType) {
		return ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, payloadType);
	}

	private static class StaticApplicationListenerMethodAdapter
			extends ApplicationListenerMethodAdapter {

		private final Object targetBean;

		public StaticApplicationListenerMethodAdapter(Method method, Object targetBean) {
			super("unused", targetBean.getClass(), method);
			this.targetBean = targetBean;
		}

		@Override
		public Object getTargetBean() {
			return this.targetBean;
		}
	}


	private static class SampleEvents {


		@EventListener
		@Order(42)
		public void handleRaw(ApplicationEvent event) {
		}

		@EventListener
		public void handleGenericString(GenericTestEvent<String> event) {
		}

		@EventListener
		public void handleString(String payload) {
		}

		@EventListener(String.class)
		public void handleStringAnnotationValue() {
		}

		@EventListener(classes = String.class)
		public void handleStringAnnotationClasses() {
		}

		@EventListener(String.class)
		public void handleStringAnnotationValueAndParameter(String payload) {
		}

		@EventListener({String.class, Integer.class})
		public void handleStringOrInteger() {
		}

		@EventListener({String.class, Integer.class})
		public void handleStringOrIntegerWithParam(String invalid) {
		}

		@EventListener
		public void handleGenericStringPayload(EntityWrapper<String> event) {
		}

		@EventListener
		public void handleGenericAnyPayload(EntityWrapper<?> event) {
		}

		@EventListener
		public void tooManyParameters(String event, String whatIsThis) {
		}

		@EventListener
		public void noParameter() {
		}

		@EventListener
		public void moreThanOneParameter(String foo, Integer bar) {
		}

		@EventListener
		public void generateRuntimeException(GenericTestEvent<String> event) {
			if ("fail".equals(event.getPayload())) {
				throw new IllegalStateException("Test exception");
			}
		}

		@EventListener
		public void generateCheckedException(GenericTestEvent<String> event) throws IOException {
			if ("fail".equals(event.getPayload())) {
				throw new IOException("Test exception");
			}
		}
	}

	interface SimpleService {

		void handleIt(ApplicationEvent event);

	}

	private static class EntityWrapper<T> implements ResolvableTypeProvider {
		private final T entity;

		public EntityWrapper(T entity) {
			this.entity = entity;
		}

		@Override
		public ResolvableType getResolvableType() {
			return ResolvableType.forClassWithGenerics(getClass(), this.entity.getClass());
		}
	}

	static class InvalidProxyTestBean implements SimpleService {

		@Override
		public void handleIt(ApplicationEvent event) {
		}

		@EventListener
		public void handleIt2(ApplicationEvent event) {
		}
	}

	@SuppressWarnings({"unused", "serial"})
	static class PayloadTestEvent<V, T> extends PayloadApplicationEvent<T> {

		private final V something;

		public PayloadTestEvent(Object source, T payload, V something) {
			super(source, payload);
			this.something = something;
		}
	}

	@SuppressWarnings({ "serial" })
	static class PayloadStringTestEvent extends PayloadTestEvent<Long, String> {
		public PayloadStringTestEvent(Object source, String payload, Long something) {
			super(source, payload, something);
		}
	}

}
