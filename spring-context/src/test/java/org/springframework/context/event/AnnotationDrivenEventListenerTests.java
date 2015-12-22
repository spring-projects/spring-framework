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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.event.test.AbstractIdentifiable;
import org.springframework.context.event.test.AnotherTestEvent;
import org.springframework.context.event.test.EventCollector;
import org.springframework.context.event.test.Identifiable;
import org.springframework.context.event.test.TestEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class AnnotationDrivenEventListenerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private ConfigurableApplicationContext context;

	private EventCollector eventCollector;

	private CountDownLatch countDownLatch; // 1 call by default


	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}


	@Test
	public void simpleEventJavaConfig() {
		load(TestEventListener.class);
		TestEvent event = new TestEvent(this, "test");
		TestEventListener listener = this.context.getBean(TestEventListener.class);
		this.eventCollector.assertNoEventReceived(listener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void simpleEventXmlConfig() {
		this.context = new ClassPathXmlApplicationContext(
				"org/springframework/context/event/simple-event-configuration.xml");
		TestEvent event = new TestEvent(this, "test");
		TestEventListener listener = this.context.getBean(TestEventListener.class);
		this.eventCollector = getEventCollector(this.context);

		this.eventCollector.assertNoEventReceived(listener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void metaAnnotationIsDiscovered() {
		load(MetaAnnotationListenerTestBean.class);

		MetaAnnotationListenerTestBean bean = context.getBean(MetaAnnotationListenerTestBean.class);
		this.eventCollector.assertNoEventReceived(bean);

		TestEvent event = new TestEvent();
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(bean, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void contextEventsAreReceived() {
		load(ContextEventListener.class);
		ContextEventListener listener = this.context.getBean(ContextEventListener.class);

		List<Object> events = this.eventCollector.getEvents(listener);
		assertEquals("Wrong number of initial context events", 1, events.size());
		assertEquals(ContextRefreshedEvent.class, events.get(0).getClass());

		this.context.stop();
		List<Object> eventsAfterStop = this.eventCollector.getEvents(listener);
		assertEquals("Wrong number of context events on shutdown", 2, eventsAfterStop.size());
		assertEquals(ContextStoppedEvent.class, eventsAfterStop.get(1).getClass());
		this.eventCollector.assertTotalEventsCount(2);
	}

	@Test
	public void methodSignatureNoEvent() {
		AnnotationConfigApplicationContext failingContext =
				new AnnotationConfigApplicationContext();
		failingContext.register(BasicConfiguration.class,
				InvalidMethodSignatureEventListener.class);

		thrown.expect(BeanInitializationException.class);
		thrown.expectMessage(InvalidMethodSignatureEventListener.class.getName());
		thrown.expectMessage("cannotBeCalled");
		failingContext.refresh();
	}

	@Test
	public void simpleReply() {
		load(TestEventListener.class, ReplyEventListener.class);
		AnotherTestEvent event = new AnotherTestEvent(this, "dummy");
		ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);


		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertNoEventReceived(replyEventListener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(replyEventListener, event);
		this.eventCollector.assertEvent(listener, new TestEvent(replyEventListener, event.getId(), "dummy")); // reply
		this.eventCollector.assertTotalEventsCount(2);
	}

	@Test
	public void nullReplyIgnored() {
		load(TestEventListener.class, ReplyEventListener.class);
		AnotherTestEvent event = new AnotherTestEvent(this, null); // No response
		ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertNoEventReceived(replyEventListener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(replyEventListener, event);
		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void arrayReply() {
		load(TestEventListener.class, ReplyEventListener.class);
		AnotherTestEvent event = new AnotherTestEvent(this, new String[]{"first", "second"});
		ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertNoEventReceived(replyEventListener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(replyEventListener, event);
		this.eventCollector.assertEvent(listener, "first", "second"); // reply
		this.eventCollector.assertTotalEventsCount(3);
	}

	@Test
	public void collectionReply() {
		load(TestEventListener.class, ReplyEventListener.class);
		Set<Object> replies = new LinkedHashSet<>();
		replies.add("first");
		replies.add(4L);
		replies.add("third");
		AnotherTestEvent event = new AnotherTestEvent(this, replies);
		ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertNoEventReceived(replyEventListener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(replyEventListener, event);
		this.eventCollector.assertEvent(listener, "first", "third"); // reply (no listener for 4L)
		this.eventCollector.assertTotalEventsCount(3);
	}

	@Test
	public void collectionReplyNullValue() {
		load(TestEventListener.class, ReplyEventListener.class);
		AnotherTestEvent event = new AnotherTestEvent(this, Arrays.asList(null, "test"));
		ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertNoEventReceived(replyEventListener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(replyEventListener, event);
		this.eventCollector.assertEvent(listener, "test");
		this.eventCollector.assertTotalEventsCount(2);
	}

	@Test
	public void eventListenerWorksWithSimpleInterfaceProxy() throws Exception {
		load(ScopedProxyTestBean.class);

		SimpleService proxy = this.context.getBean(SimpleService.class);
		assertTrue("bean should be a proxy", proxy instanceof Advised);
		this.eventCollector.assertNoEventReceived(proxy.getId());

		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		this.eventCollector.assertNoEventReceived(proxy.getId());

		TestEvent event = new TestEvent();
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(proxy.getId(), event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void eventListenerWorksWithAnnotatedInterfaceProxy() throws Exception {
		load(AnnotatedProxyTestBean.class);

		AnnotatedSimpleService proxy = this.context.getBean(AnnotatedSimpleService.class);
		assertTrue("bean should be a proxy", proxy instanceof Advised);
		this.eventCollector.assertNoEventReceived(proxy.getId());

		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		this.eventCollector.assertNoEventReceived(proxy.getId());

		TestEvent event = new TestEvent();
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(proxy.getId(), event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void eventListenerWorksWithCglibProxy() throws Exception {
		load(CglibProxyTestBean.class);

		CglibProxyTestBean proxy = this.context.getBean(CglibProxyTestBean.class);
		assertTrue("bean should be a cglib proxy", AopUtils.isCglibProxy(proxy));
		this.eventCollector.assertNoEventReceived(proxy.getId());

		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		this.eventCollector.assertNoEventReceived(proxy.getId());

		TestEvent event = new TestEvent();
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(proxy.getId(), event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void eventListenerWorksWithCustomScope() throws Exception {
		load(CustomScopeTestBean.class);
		CustomScope customScope = new CustomScope();
		this.context.getBeanFactory().registerScope("custom", customScope);

		CustomScopeTestBean proxy = this.context.getBean(CustomScopeTestBean.class);
		assertTrue("bean should be a cglib proxy", AopUtils.isCglibProxy(proxy));
		this.eventCollector.assertNoEventReceived(proxy.getId());

		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		this.eventCollector.assertNoEventReceived(proxy.getId());

		customScope.active = false;
		this.context.publishEvent(new ContextRefreshedEvent(this.context));
		customScope.active = true;
		this.eventCollector.assertNoEventReceived(proxy.getId());

		TestEvent event = new TestEvent();
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(proxy.getId(), event);
		this.eventCollector.assertTotalEventsCount(1);

		try {
			customScope.active = false;
			this.context.publishEvent(new TestEvent());
			fail("Should have thrown IllegalStateException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}
	}

	@Test
	public void asyncProcessingApplied() throws InterruptedException {
		loadAsync(AsyncEventListener.class);

		String threadName = Thread.currentThread().getName();
		AnotherTestEvent event = new AnotherTestEvent(this, threadName);
		AsyncEventListener listener = this.context.getBean(AsyncEventListener.class);
		this.eventCollector.assertNoEventReceived(listener);

		this.context.publishEvent(event);
		countDownLatch.await(2, TimeUnit.SECONDS);
		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void asyncProcessingAppliedWithInterfaceProxy() throws InterruptedException {
		doLoad(AsyncConfigurationWithInterfaces.class, SimpleProxyTestBean.class);

		String threadName = Thread.currentThread().getName();
		AnotherTestEvent event = new AnotherTestEvent(this, threadName);
		SimpleService listener = this.context.getBean(SimpleService.class);
		this.eventCollector.assertNoEventReceived(listener);

		this.context.publishEvent(event);
		countDownLatch.await(2, TimeUnit.SECONDS);
		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void asyncProcessingAppliedWithScopedProxy() throws InterruptedException {
		doLoad(AsyncConfigurationWithInterfaces.class, ScopedProxyTestBean.class);

		String threadName = Thread.currentThread().getName();
		AnotherTestEvent event = new AnotherTestEvent(this, threadName);
		SimpleService listener = this.context.getBean(SimpleService.class);
		this.eventCollector.assertNoEventReceived(listener);

		this.context.publishEvent(event);
		countDownLatch.await(2, TimeUnit.SECONDS);
		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void exceptionPropagated() {
		load(ExceptionEventListener.class);
		TestEvent event = new TestEvent(this, "fail");
		ExceptionEventListener listener = this.context.getBean(ExceptionEventListener.class);
		this.eventCollector.assertNoEventReceived(listener);
		try {
			this.context.publishEvent(event);
			fail("An exception should have thrown");
		}
		catch (IllegalStateException e) {
			assertEquals("Wrong exception", "Test exception", e.getMessage());
			this.eventCollector.assertEvent(listener, event);
			this.eventCollector.assertTotalEventsCount(1);
		}
	}

	@Test
	public void exceptionNotPropagatedWithAsync() throws InterruptedException {
		loadAsync(ExceptionEventListener.class);
		AnotherTestEvent event = new AnotherTestEvent(this, "fail");
		ExceptionEventListener listener = this.context.getBean(ExceptionEventListener.class);
		this.eventCollector.assertNoEventReceived(listener);

		this.context.publishEvent(event);
		countDownLatch.await(2, TimeUnit.SECONDS);

		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void listenerWithSimplePayload() {
		load(TestEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.context.publishEvent("test");
		this.eventCollector.assertEvent(listener, "test");
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void listenerWithNonMatchingPayload() {
		load(TestEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.context.publishEvent(123L);
		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertTotalEventsCount(0);
	}

	@Test
	public void replyWithPayload() {
		load(TestEventListener.class, ReplyEventListener.class);
		AnotherTestEvent event = new AnotherTestEvent(this, "String");
		ReplyEventListener replyEventListener = this.context.getBean(ReplyEventListener.class);
		TestEventListener listener = this.context.getBean(TestEventListener.class);


		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertNoEventReceived(replyEventListener);
		this.context.publishEvent(event);
		this.eventCollector.assertEvent(replyEventListener, event);
		this.eventCollector.assertEvent(listener, "String"); // reply
		this.eventCollector.assertTotalEventsCount(2);
	}

	@Test
	public void listenerWithGenericApplicationEvent() {
		load(GenericEventListener.class);
		GenericEventListener listener = this.context.getBean(GenericEventListener.class);

		this.eventCollector.assertNoEventReceived(listener);
		this.context.publishEvent("TEST");
		this.eventCollector.assertEvent(listener, "TEST");
		this.eventCollector.assertTotalEventsCount(1);
	}

	@Test
	public void conditionMatch() {
		long timestamp = System.currentTimeMillis();
		load(ConditionalEventListener.class);
		TestEvent event = new TestEvent(this, "OK");
		TestEventListener listener = this.context.getBean(ConditionalEventListener.class);
		this.eventCollector.assertNoEventReceived(listener);

		this.context.publishEvent(event);
		this.eventCollector.assertEvent(listener, event);
		this.eventCollector.assertTotalEventsCount(1);

		this.context.publishEvent("OK");
		this.eventCollector.assertEvent(listener, event, "OK");
		this.eventCollector.assertTotalEventsCount(2);

		this.context.publishEvent(timestamp);
		this.eventCollector.assertEvent(listener, event, "OK", timestamp);
		this.eventCollector.assertTotalEventsCount(3);

		this.context.publishEvent(42d);
		this.eventCollector.assertEvent(listener, event, "OK", timestamp, 42d);
		this.eventCollector.assertTotalEventsCount(4);
	}

	@Test
	public void conditionDoesNotMatch() {
		long maxLong = Long.MAX_VALUE;
		load(ConditionalEventListener.class);
		TestEvent event = new TestEvent(this, "KO");
		TestEventListener listener = this.context.getBean(ConditionalEventListener.class);
		this.eventCollector.assertNoEventReceived(listener);

		this.context.publishEvent(event);
		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertTotalEventsCount(0);

		this.context.publishEvent("KO");
		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertTotalEventsCount(0);

		this.context.publishEvent(maxLong);
		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertTotalEventsCount(0);

		this.context.publishEvent(24d);
		this.eventCollector.assertNoEventReceived(listener);
		this.eventCollector.assertTotalEventsCount(0);
	}

	@Test
	public void orderedListeners() {
		load(OrderedTestListener.class);
		OrderedTestListener listener = this.context.getBean(OrderedTestListener.class);

		assertTrue(listener.order.isEmpty());
		this.context.publishEvent("whatever");
		assertThat(listener.order, contains("first", "second", "third"));
	}


	private void load(Class<?>... classes) {
		List<Class<?>> allClasses = new ArrayList<>();
		allClasses.add(BasicConfiguration.class);
		allClasses.addAll(Arrays.asList(classes));
		doLoad(allClasses.toArray(new Class<?>[allClasses.size()]));
	}

	private void loadAsync(Class<?>... classes) {
		List<Class<?>> allClasses = new ArrayList<>();
		allClasses.add(AsyncConfiguration.class);
		allClasses.addAll(Arrays.asList(classes));
		doLoad(allClasses.toArray(new Class<?>[allClasses.size()]));
	}

	private void doLoad(Class<?>... classes) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(classes);
		this.eventCollector = ctx.getBean(EventCollector.class);
		this.countDownLatch = ctx.getBean(CountDownLatch.class);
		this.context = ctx;
	}

	private EventCollector getEventCollector(ConfigurableApplicationContext context) {
		return context.getBean(EventCollector.class);
	}


	@Configuration
	static class BasicConfiguration {

		@Bean
		public EventCollector eventCollector() {
			return new EventCollector();
		}

		@Bean
		public CountDownLatch testCountDownLatch() {
			return new CountDownLatch(1);
		}

		@Bean
		public TestConditionEvaluator conditionEvaluator() {
			return new TestConditionEvaluator();
		}

		static class TestConditionEvaluator {

			public boolean valid(Double ratio) {
				return new Double(42).equals(ratio);
			}
		}
	}


	static abstract class AbstractTestEventListener extends AbstractIdentifiable {

		@Autowired
		private EventCollector eventCollector;

		protected void collectEvent(Object content) {
			this.eventCollector.addEvent(this, content);
		}
	}


	@Component
	static class TestEventListener extends AbstractTestEventListener {

		@EventListener
		public void handle(TestEvent event) {
			collectEvent(event);
		}

		@EventListener
		public void handleString(String content) {
			collectEvent(content);
		}
	}


	@EventListener
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface FooListener {
	}


	@Component
	static class MetaAnnotationListenerTestBean extends AbstractTestEventListener {

		@FooListener
		public void handleIt(TestEvent event) {
			collectEvent(event);
		}
	}


	@Component
	static class ContextEventListener extends AbstractTestEventListener {

		@EventListener
		public void handleContextEvent(ApplicationContextEvent event) {
			collectEvent(event);
		}

	}


	@Component
	static class InvalidMethodSignatureEventListener {

		@EventListener
		public void cannotBeCalled(String s, Integer what) {
		}
	}


	@Component
	static class ReplyEventListener extends AbstractTestEventListener {

		@EventListener
		public Object handle(AnotherTestEvent event) {
			collectEvent(event);
			if (event.content == null) {
				return null;
			}
			else if (event.content instanceof String) {
				String s = (String) event.content;
				if (s.equals("String")) {
					return event.content;
				}
				else {
					return new TestEvent(this, event.getId(), s);
				}
			}
			return event.content;
		}
	}


	@Component
	static class ExceptionEventListener extends AbstractTestEventListener {

		@Autowired
		private CountDownLatch countDownLatch;

		@EventListener
		public void handle(TestEvent event) {
			collectEvent(event);
			if ("fail".equals(event.msg)) {
				throw new IllegalStateException("Test exception");
			}
		}

		@EventListener
		@Async
		public void handleAsync(AnotherTestEvent event) {
			collectEvent(event);
			if ("fail".equals(event.content)) {
				countDownLatch.countDown();
				throw new IllegalStateException("Test exception");
			}
		}
	}


	@Component
	static class AsyncEventListener extends AbstractTestEventListener {

		@Autowired
		private CountDownLatch countDownLatch;

		@EventListener
		@Async
		public void handleAsync(AnotherTestEvent event) {
			assertTrue(!Thread.currentThread().getName().equals(event.content));
			collectEvent(event);
			countDownLatch.countDown();
		}
	}


	@Configuration
	@Import(BasicConfiguration.class)
	@EnableAsync(proxyTargetClass = true)
	static class AsyncConfiguration {
	}


	@Configuration
	@Import(BasicConfiguration.class)
	@EnableAsync(proxyTargetClass = false)
	static class AsyncConfigurationWithInterfaces {
	}


	interface SimpleService extends Identifiable {

		void handleIt(TestEvent event);

		void handleAsync(AnotherTestEvent event);
	}


	@Component
	static class SimpleProxyTestBean extends AbstractIdentifiable implements SimpleService {

		@Autowired
		private EventCollector eventCollector;

		@Autowired
		private CountDownLatch countDownLatch;

		@EventListener
		@Override
		public void handleIt(TestEvent event) {
			eventCollector.addEvent(this, event);
		}

		@EventListener
		@Async
		public void handleAsync(AnotherTestEvent event) {
			assertTrue(!Thread.currentThread().getName().equals(event.content));
			eventCollector.addEvent(this, event);
			countDownLatch.countDown();
		}
	}


	@Component
	@Scope(proxyMode = ScopedProxyMode.INTERFACES)
	static class ScopedProxyTestBean extends AbstractIdentifiable implements SimpleService {

		@Autowired
		private EventCollector eventCollector;

		@Autowired
		private CountDownLatch countDownLatch;

		@EventListener
		@Override
		public void handleIt(TestEvent event) {
			eventCollector.addEvent(this, event);
		}

		@EventListener
		@Async
		public void handleAsync(AnotherTestEvent event) {
			assertTrue(!Thread.currentThread().getName().equals(event.content));
			eventCollector.addEvent(this, event);
			countDownLatch.countDown();
		}
	}


	interface AnnotatedSimpleService extends Identifiable {

		@EventListener
		void handleIt(TestEvent event);
	}


	@Component
	@Scope(proxyMode = ScopedProxyMode.INTERFACES)
	static class AnnotatedProxyTestBean extends AbstractIdentifiable implements AnnotatedSimpleService {

		@Autowired
		private EventCollector eventCollector;

		@Override
		public void handleIt(TestEvent event) {
			eventCollector.addEvent(this, event);
		}
	}


	@Component
	@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class CglibProxyTestBean extends AbstractTestEventListener {

		@EventListener
		public void handleIt(TestEvent event) {
			collectEvent(event);
		}
	}


	@Component
	@Scope(scopeName = "custom", proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class CustomScopeTestBean extends AbstractTestEventListener {

		@EventListener
		public void handleIt(TestEvent event) {
			collectEvent(event);
		}
	}


	@Component
	static class GenericEventListener extends AbstractTestEventListener {

		@EventListener
		public void handleString(PayloadApplicationEvent<String> event) {
			collectEvent(event.getPayload());
		}
	}


	@Component
	static class ConditionalEventListener extends TestEventListener {

		@EventListener(condition = "'OK'.equals(#root.event.msg)")
		@Override
		public void handle(TestEvent event) {
			super.handle(event);
		}

		@Override
		@EventListener(condition = "#payload.startsWith('OK')")
		public void handleString(String payload) {
			super.handleString(payload);
		}

		@EventListener(condition = "#root.event.timestamp > #p0")
		public void handleTimestamp(Long timestamp) {
			collectEvent(timestamp);
		}

		@EventListener(condition = "@conditionEvaluator.valid(#p0)")
		public void handleRatio(Double ratio) {
			collectEvent(ratio);
		}
	}


	@Component
	static class OrderedTestListener extends TestEventListener {

		public final List<String> order = new ArrayList<>();

		@EventListener
		@Order(50)
		public void handleThird(String payload) {
			order.add("third");
		}

		@EventListener
		@Order(-50)
		public void handleFirst(String payload) {
			order.add("first");
		}

		@EventListener
		public void handleSecond(String payload) {
			order.add("second");
		}
	}


	private static class CustomScope implements org.springframework.beans.factory.config.Scope {

		public boolean active = true;

		private Object instance = null;

		@Override
		public Object get(String name, ObjectFactory<?> objectFactory) {
			Assert.state(this.active, "Not active");
			if (this.instance == null) {
				this.instance = objectFactory.getObject();
			}
			return this.instance;
		}

		@Override
		public Object remove(String name) {
			return null;
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback) {
		}

		@Override
		public Object resolveContextualObject(String key) {
			return null;
		}

		@Override
		public String getConversationId() {
			return null;
		}
	}

}
