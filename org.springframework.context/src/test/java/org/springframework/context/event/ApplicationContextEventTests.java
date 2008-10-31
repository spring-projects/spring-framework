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

import org.aopalliance.intercept.MethodInvocation;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Assert;

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

/**
 * Unit and integration tests for the ApplicationContext event support.
 *
 * @author Alef Arendsen
 * @author Rick Evans
 */
public class ApplicationContextEventTests {

	private AbstractApplicationEventMulticaster getMulticaster() {
		return new AbstractApplicationEventMulticaster() {
			public void multicastEvent(ApplicationEvent event) {
			}
		};
	}

	@Test
	public void multicasterNewCollectionClass() {
		AbstractApplicationEventMulticaster mc = getMulticaster();

		mc.addApplicationListener(new NoOpApplicationListener());

		mc.setCollectionClass(ArrayList.class);

		assertEquals(1, mc.getApplicationListeners().size());
		assertEquals(ArrayList.class, mc.getApplicationListeners().getClass());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multicasterInvalidCollectionClass_NotEvenACollectionType() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		mc.setCollectionClass(ApplicationContextEventTests.class);
	}

	@Test(expected = FatalBeanException.class)
	public void multicasterInvalidCollectionClass_PassingAnInterfaceNotAConcreteClass() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		mc.setCollectionClass(List.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multicasterNullCollectionClass() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		mc.setCollectionClass(null);
	}

	@Test
	public void multicasterRemoveAll() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		mc.addApplicationListener(new NoOpApplicationListener());
		mc.removeAllListeners();

		assertEquals(0, mc.getApplicationListeners().size());
	}

	@Test
	public void multicasterRemoveOne() {
		AbstractApplicationEventMulticaster mc = getMulticaster();
		ApplicationListener one = new NoOpApplicationListener();
		ApplicationListener two = new NoOpApplicationListener();
		mc.addApplicationListener(one);
		mc.addApplicationListener(two);

		mc.removeApplicationListener(one);

		assertEquals(1, mc.getApplicationListeners().size());
		assertTrue("Remaining listener present", mc.getApplicationListeners().contains(two));
	}

	@Test
	public void simpleApplicationEventMulticaster() {
		ApplicationListener listener = EasyMock.createMock(ApplicationListener.class);

		ApplicationEvent evt = new ContextClosedEvent(new StaticApplicationContext());
		listener.onApplicationEvent(evt);

		SimpleApplicationEventMulticaster smc = new SimpleApplicationEventMulticaster();
		smc.addApplicationListener(listener);

		replay(listener);

		smc.multicastEvent(evt);

		verify(listener);
	}

	@Test
	public void testEvenPublicationInterceptor() throws Throwable {
		MethodInvocation invocation = EasyMock.createMock(MethodInvocation.class);
		ApplicationContext ctx = EasyMock.createMock(ApplicationContext.class);

		EventPublicationInterceptor interceptor = new EventPublicationInterceptor();
		interceptor.setApplicationEventClass(MyEvent.class);
		interceptor.setApplicationEventPublisher(ctx);
		interceptor.afterPropertiesSet();

		expect(invocation.proceed()).andReturn(new Object());

		expect(invocation.getThis()).andReturn(new Object());

		ctx.publishEvent(isA(MyEvent.class));

		replay(invocation, ctx);

		interceptor.invoke(invocation);

		verify(invocation, ctx);
	}

	@Test
	public void listenerAndBroadcasterWithUnresolvableCircularReference() {
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

	@Test
	public void listenerAndBroadcasterWithResolvableCircularReference() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("broadcaster", new RootBeanDefinition(BeanThatBroadcasts.class));
		RootBeanDefinition listenerDef = new RootBeanDefinition(BeanThatListens.class);
		listenerDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference("broadcaster"));
		context.registerBeanDefinition("listener", listenerDef);
		context.refresh();

		BeanThatBroadcasts broadcaster = (BeanThatBroadcasts) context.getBean("broadcaster");
		context.publishEvent(new MyEvent(context));
		Assert.assertEquals("The event was not received by the listener", 2, broadcaster.receivedCount);
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
