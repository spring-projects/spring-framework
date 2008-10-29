/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;
import org.easymock.MockControl;
import org.easymock.internal.AlwaysMatcher;
import org.easymock.internal.EqualsMatcher;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.BeanThatBroadcasts;
import org.springframework.context.BeanThatListens;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.AssertThrows;

/**
 * Unit and integration tests for the ApplicationContext event support.
 *
 * @author Alef Arendsen
 * @author Rick Evans
 */
public class ApplicationContextEventTests extends TestCase {

	private AbstractApplicationEventMulticaster getMulticaster() {
		return new AbstractApplicationEventMulticaster() {
			public void multicastEvent(ApplicationEvent event) {
			}
		};
	}

	public void testMulticasterNewCollectionClass() {
		AbstractApplicationEventMulticaster mc = getMulticaster();

		mc.addApplicationListener(new NoOpApplicationListener());

		mc.setCollectionClass(ArrayList.class);

		assertEquals(1, mc.getApplicationListeners().size());
		assertEquals(ArrayList.class, mc.getApplicationListeners().getClass());
	}

	public void testMulticasterInvalidCollectionClass_NotEvenACollectionType() {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				AbstractApplicationEventMulticaster mc = getMulticaster();
				mc.setCollectionClass(ApplicationContextEventTests.class);
			}
		}.runTest();
	}

	public void testMulticasterInvalidCollectionClass_PassingAnInterfaceNotAConcreteClass() {
		new AssertThrows(FatalBeanException.class) {
			public void test() throws Exception {
				AbstractApplicationEventMulticaster mc = getMulticaster();
				mc.setCollectionClass(List.class);
			}
		}.runTest();
	}

	public void testMulticasterNullCollectionClass() {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				AbstractApplicationEventMulticaster mc = getMulticaster();
				mc.setCollectionClass(null);
			}
		}.runTest();
	}

	public void testMulticasterRemoveAll() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		mc.addApplicationListener(new NoOpApplicationListener());
		mc.removeAllListeners();

		assertEquals(0, mc.getApplicationListeners().size());
	}

	public void testMulticasterRemoveOne() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		ApplicationListener one = new NoOpApplicationListener();
		ApplicationListener two = new NoOpApplicationListener();
		mc.addApplicationListener(one);
		mc.addApplicationListener(two);

		mc.removeApplicationListener(one);

		assertEquals(1, mc.getApplicationListeners().size());
		assertTrue("Remaining listener present", mc.getApplicationListeners().contains(two));
	}

	public void testSimpleApplicationEventMulticaster() {
		MockControl ctrl = MockControl.createControl(ApplicationListener.class);
		ApplicationListener listener = (ApplicationListener) ctrl.getMock();

		ApplicationEvent evt = new ContextClosedEvent(new StaticApplicationContext());
		listener.onApplicationEvent(evt);
		ctrl.setMatcher(new EqualsMatcher());

		SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
		smc.addApplicationListener(listener);

		ctrl.replay();

		smc.multicastEvent(evt);

		ctrl.verify();
	}

	public void testEvenPublicationInterceptor() throws Throwable {
		MockControl invCtrl = MockControl.createControl(MethodInvocation.class);
		MethodInvocation invocation = (MethodInvocation) invCtrl.getMock();

		MockControl ctxCtrl = MockControl.createControl(ApplicationContext.class);
		ApplicationContext ctx = (ApplicationContext) ctxCtrl.getMock();

		EventPublicationInterceptor interceptor =
				new EventPublicationInterceptor();
		interceptor.setApplicationEventClass(MyEvent.class);
		interceptor.setApplicationEventPublisher(ctx);
		interceptor.afterPropertiesSet();

		invocation.proceed();
		invCtrl.setReturnValue(new Object());

		invocation.getThis();
		invCtrl.setReturnValue(new Object());
		ctx.publishEvent(new MyEvent(new Object()));
		ctxCtrl.setDefaultMatcher(new AlwaysMatcher());

		ctxCtrl.replay();
		invCtrl.replay();

		interceptor.invoke(invocation);

		ctxCtrl.verify();
		invCtrl.verify();
	}

	public void testListenerAndBroadcasterWithUnresolvableCircularReference() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.setDisplayName("listener context");
		context.registerBeanDefinition("broadcaster", new RootBeanDefinition(BeanThatBroadcasts.class));
		RootBeanDefinition listenerDef = new RootBeanDefinition(BeanThatListens.class);
		listenerDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("broadcaster"));
		context.registerBeanDefinition("listener", listenerDef);
		try {
			context.refresh();
			fail("Should have thrown BeanCreationException with nested BeanCurrentlyInCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(BeanCurrentlyInCreationException.class));
		}
	}

	public void testListenerAndBroadcasterWithResolvableCircularReference() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("broadcaster", new RootBeanDefinition(BeanThatBroadcasts.class));
		RootBeanDefinition listenerDef = new RootBeanDefinition(BeanThatListens.class);
		listenerDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("broadcaster"));
		context.registerBeanDefinition("listener", listenerDef);
		context.refresh();

		BeanThatBroadcasts broadcaster = (BeanThatBroadcasts) context.getBean("broadcaster");
		context.publishEvent(new MyEvent(context));
		assertEquals("The event was not received by the listener", 2, broadcaster.receivedCount);
	}


	public static class MyEvent extends ApplicationEvent {

		public MyEvent(Object source) {
			super(source);
		}
	}


	private static final class NoOpApplicationListener implements ApplicationListener {

		public void onApplicationEvent(ApplicationEvent event) {
		}

	}

}
