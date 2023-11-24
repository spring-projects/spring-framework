/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.test.TestEvent;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.context.testfixture.beans.TestApplicationListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 */
class EventPublicationInterceptorTests {

	private final EventPublicationInterceptor interceptor = new EventPublicationInterceptor();


	@BeforeEach
	void setup() {
		ApplicationEventPublisher publisher = mock();
		this.interceptor.setApplicationEventPublisher(publisher);
	}


	@Test
	void withNoApplicationEventClassSupplied() {
		assertThatIllegalArgumentException().isThrownBy(interceptor::afterPropertiesSet);
	}

	@Test
	void withNonApplicationEventClassSupplied() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
				interceptor.setApplicationEventClass(getClass());
				interceptor.afterPropertiesSet();
		});
	}

	@Test
	void withAbstractStraightApplicationEventClassSupplied() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
				interceptor.setApplicationEventClass(ApplicationEvent.class);
				interceptor.afterPropertiesSet();
		});
	}

	@Test
	void withApplicationEventClassThatDoesntExposeAValidCtor() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
				interceptor.setApplicationEventClass(TestEventWithNoValidOneArgObjectCtor.class);
				interceptor.afterPropertiesSet();
		});
	}

	@Test
	void expectedBehavior() {
		TestBean target = new TestBean();
		final TestApplicationListener listener = new TestApplicationListener();

		class TestContext extends StaticApplicationContext {
			@Override
			protected void onRefresh() throws BeansException {
				addApplicationListener(listener);
			}
		}

		StaticApplicationContext ctx = new TestContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("applicationEventClass", TestEvent.class.getName());
		// should automatically receive applicationEventPublisher reference
		ctx.registerSingleton("publisher", EventPublicationInterceptor.class, pvs);
		ctx.registerSingleton("otherListener", FactoryBeanTestListener.class);
		ctx.refresh();

		EventPublicationInterceptor interceptor =
				(EventPublicationInterceptor) ctx.getBean("publisher");
		ProxyFactory factory = new ProxyFactory(target);
		factory.addAdvice(0, interceptor);

		ITestBean testBean = (ITestBean) factory.getProxy();

		// invoke any method on the advised proxy to see if the interceptor has been invoked
		testBean.getAge();

		// two events: ContextRefreshedEvent and TestEvent
		assertThat(listener.getEventCount()).as("Interceptor must have published 2 events").isEqualTo(2);
		TestApplicationListener otherListener = (TestApplicationListener) ctx.getBean("&otherListener");
		assertThat(otherListener.getEventCount()).as("Interceptor must have published 2 events").isEqualTo(2);
		ctx.close();
	}


	@SuppressWarnings("serial")
	static final class TestEventWithNoValidOneArgObjectCtor extends ApplicationEvent {

		public TestEventWithNoValidOneArgObjectCtor() {
			super("");
		}
	}


	static class FactoryBeanTestListener extends TestApplicationListener implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return "test";
		}

		@Override
		public Class<String> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}
