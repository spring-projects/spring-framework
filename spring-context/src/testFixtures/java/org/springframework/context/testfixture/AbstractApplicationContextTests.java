/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.context.testfixture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.testfixture.beans.LifecycleBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.factory.xml.AbstractListableBeanFactoryTests;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.testfixture.beans.ACATester;
import org.springframework.context.testfixture.beans.BeanThatListens;
import org.springframework.context.testfixture.beans.TestApplicationListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public abstract class AbstractApplicationContextTests extends AbstractListableBeanFactoryTests {

	protected ConfigurableApplicationContext applicationContext;

	/** Subclass must register this */
	protected TestApplicationListener listener = new TestApplicationListener();

	protected TestApplicationListener parentListener = new TestApplicationListener();


	@BeforeEach
	protected void setup() throws Exception {
		this.applicationContext = createContext();
	}

	@Override
	protected BeanFactory getBeanFactory() {
		return this.applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * Must register a TestListener.
	 * Must register standard beans.
	 * Parent must register rod with name Roderick
	 * and father with name Albert.
	 */
	protected abstract ConfigurableApplicationContext createContext() throws Exception;


	@Test
	protected void contextAwareSingletonWasCalledBack() {
		ACATester aca = (ACATester) applicationContext.getBean("aca");
		assertThat(aca.getApplicationContext()).as("has had context set").isSameAs(applicationContext);
		Object aca2 = applicationContext.getBean("aca");
		assertThat(aca).as("Same instance").isSameAs(aca2);
		assertThat(applicationContext.isSingleton("aca")).as("Says is singleton").isTrue();
	}

	@Test
	protected void contextAwarePrototypeWasCalledBack() {
		ACATester aca = (ACATester) applicationContext.getBean("aca-prototype");
		assertThat(aca.getApplicationContext()).as("has had context set").isSameAs(applicationContext);
		Object aca2 = applicationContext.getBean("aca-prototype");
		assertThat(aca).as("NOT Same instance").isNotSameAs(aca2);
		boolean condition = !applicationContext.isSingleton("aca-prototype");
		assertThat(condition).as("Says is prototype").isTrue();
	}

	@Test
	protected void parentNonNull() {
		assertThat(applicationContext.getParent()).as("parent isn't null").isNotNull();
	}

	@Test
	protected void grandparentNull() {
		assertThat(applicationContext.getParent().getParent()).as("grandparent is null").isNull();
	}

	@Test
	protected void overrideWorked() {
		TestBean rod = (TestBean) applicationContext.getParent().getBean("rod");
		assertThat(rod.getName().equals("Roderick")).as("Parent's name differs").isTrue();
	}

	@Test
	protected void grandparentDefinitionFound() {
		TestBean dad = (TestBean) applicationContext.getBean("father");
		assertThat(dad.getName().equals("Albert")).as("Dad has correct name").isTrue();
	}

	@Test
	protected void grandparentTypedDefinitionFound() {
		TestBean dad = applicationContext.getBean("father", TestBean.class);
		assertThat(dad.getName().equals("Albert")).as("Dad has correct name").isTrue();
	}

	@Test
	protected void closeTriggersDestroy() {
		LifecycleBean lb = (LifecycleBean) applicationContext.getBean("lifecycle");
		boolean condition = !lb.isDestroyed();
		assertThat(condition).as("Not destroyed").isTrue();
		applicationContext.close();
		if (applicationContext.getParent() != null) {
			((ConfigurableApplicationContext) applicationContext.getParent()).close();
		}
		assertThat(lb.isDestroyed()).as("Destroyed").isTrue();
		applicationContext.close();
		if (applicationContext.getParent() != null) {
			((ConfigurableApplicationContext) applicationContext.getParent()).close();
		}
		assertThat(lb.isDestroyed()).as("Destroyed").isTrue();
	}

	@Test
	protected void messageSource() throws NoSuchMessageException {
		assertThat(applicationContext.getMessage("code1", null, Locale.getDefault())).isEqualTo("message1");
		assertThat(applicationContext.getMessage("code2", null, Locale.getDefault())).isEqualTo("message2");
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				applicationContext.getMessage("code0", null, Locale.getDefault()));
	}

	@Test
	protected void events() throws Exception {
		doTestEvents(this.listener, this.parentListener, new MyEvent(this));
	}

	@Test
	protected void eventsWithNoSource() throws Exception {
		// See SPR-10945 Serialized events result in a null source
		MyEvent event = new MyEvent(this);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(event);
		oos.close();
		event = (MyEvent) new ObjectInputStream(new ByteArrayInputStream(
				bos.toByteArray())).readObject();
		doTestEvents(this.listener, this.parentListener, event);
	}

	protected void doTestEvents(TestApplicationListener listener, TestApplicationListener parentListener,
			MyEvent event) {
		listener.zeroCounter();
		parentListener.zeroCounter();
		assertThat(listener.getEventCount()).as("0 events before publication").isEqualTo(0);
		assertThat(parentListener.getEventCount()).as("0 parent events before publication").isEqualTo(0);
		this.applicationContext.publishEvent(event);
		assertThat(listener.getEventCount()).as("1 events after publication, not " + listener.getEventCount())
				.isEqualTo(1);
		assertThat(parentListener.getEventCount()).as("1 parent events after publication").isEqualTo(1);
	}

	@Test
	protected void beanAutomaticallyHearsEvents() {
		//String[] listenerNames = ((ListableBeanFactory) applicationContext).getBeanDefinitionNames(ApplicationListener.class);
		//assertTrue("listeners include beanThatListens", Arrays.asList(listenerNames).contains("beanThatListens"));
		BeanThatListens b = (BeanThatListens) applicationContext.getBean("beanThatListens");
		b.zero();
		assertThat(b.getEventCount()).as("0 events before publication").isEqualTo(0);
		this.applicationContext.publishEvent(new MyEvent(this));
		assertThat(b.getEventCount()).as("1 events after publication, not " + b.getEventCount()).isEqualTo(1);
	}


	@SuppressWarnings("serial")
	public static class MyEvent extends ApplicationEvent {

		public MyEvent(Object source) {
			super(source);
		}
	}

}
