/*
 * Copyright 2002-2019 the original author or authors.
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

	/** Must be supplied as XML */
	public static final String TEST_NAMESPACE = "testNamespace";

	protected ConfigurableApplicationContext applicationContext;

	/** Subclass must register this */
	protected TestApplicationListener listener = new TestApplicationListener();

	protected TestApplicationListener parentListener = new TestApplicationListener();

	@BeforeEach
	public void setUp() throws Exception {
		this.applicationContext = createContext();
	}

	@Override
	protected BeanFactory getBeanFactory() {
		return applicationContext;
	}

	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * Must register a TestListener.
	 * Must register standard beans.
	 * Parent must register rod with name Roderick
	 * and father with name Albert.
	 */
	protected abstract ConfigurableApplicationContext createContext() throws Exception;

	@Test
	public void contextAwareSingletonWasCalledBack() throws Exception {
		ACATester aca = (ACATester) applicationContext.getBean("aca");
		assertThat(aca.getApplicationContext() == applicationContext).as("has had context set").isTrue();
		Object aca2 = applicationContext.getBean("aca");
		assertThat(aca == aca2).as("Same instance").isTrue();
		assertThat(applicationContext.isSingleton("aca")).as("Says is singleton").isTrue();
	}

	@Test
	public void contextAwarePrototypeWasCalledBack() throws Exception {
		ACATester aca = (ACATester) applicationContext.getBean("aca-prototype");
		assertThat(aca.getApplicationContext() == applicationContext).as("has had context set").isTrue();
		Object aca2 = applicationContext.getBean("aca-prototype");
		assertThat(aca != aca2).as("NOT Same instance").isTrue();
		boolean condition = !applicationContext.isSingleton("aca-prototype");
		assertThat(condition).as("Says is prototype").isTrue();
	}

	@Test
	public void parentNonNull() {
		assertThat(applicationContext.getParent() != null).as("parent isn't null").isTrue();
	}

	@Test
	public void grandparentNull() {
		assertThat(applicationContext.getParent().getParent() == null).as("grandparent is null").isTrue();
	}

	@Test
	public void overrideWorked() throws Exception {
		TestBean rod = (TestBean) applicationContext.getParent().getBean("rod");
		assertThat(rod.getName().equals("Roderick")).as("Parent's name differs").isTrue();
	}

	@Test
	public void grandparentDefinitionFound() throws Exception {
		TestBean dad = (TestBean) applicationContext.getBean("father");
		assertThat(dad.getName().equals("Albert")).as("Dad has correct name").isTrue();
	}

	@Test
	public void grandparentTypedDefinitionFound() throws Exception {
		TestBean dad = applicationContext.getBean("father", TestBean.class);
		assertThat(dad.getName().equals("Albert")).as("Dad has correct name").isTrue();
	}

	@Test
	public void closeTriggersDestroy() {
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
	public void messageSource() throws NoSuchMessageException {
		assertThat(applicationContext.getMessage("code1", null, Locale.getDefault())).isEqualTo("message1");
		assertThat(applicationContext.getMessage("code2", null, Locale.getDefault())).isEqualTo("message2");
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				applicationContext.getMessage("code0", null, Locale.getDefault()));
	}

	@Test
	public void events() throws Exception {
		doTestEvents(this.listener, this.parentListener, new MyEvent(this));
	}

	@Test
	public void eventsWithNoSource() throws Exception {
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
		assertThat(listener.getEventCount() == 0).as("0 events before publication").isTrue();
		assertThat(parentListener.getEventCount() == 0).as("0 parent events before publication").isTrue();
		this.applicationContext.publishEvent(event);
		assertThat(listener.getEventCount() == 1).as("1 events after publication, not " + listener.getEventCount()).isTrue();
		assertThat(parentListener.getEventCount() == 1).as("1 parent events after publication").isTrue();
	}

	@Test
	public void beanAutomaticallyHearsEvents() throws Exception {
		//String[] listenerNames = ((ListableBeanFactory) applicationContext).getBeanDefinitionNames(ApplicationListener.class);
		//assertTrue("listeners include beanThatListens", Arrays.asList(listenerNames).contains("beanThatListens"));
		BeanThatListens b = (BeanThatListens) applicationContext.getBean("beanThatListens");
		b.zero();
		assertThat(b.getEventCount() == 0).as("0 events before publication").isTrue();
		this.applicationContext.publishEvent(new MyEvent(this));
		assertThat(b.getEventCount() == 1).as("1 events after publication, not " + b.getEventCount()).isTrue();
	}


	@SuppressWarnings("serial")
	public static class MyEvent extends ApplicationEvent {

		public MyEvent(Object source) {
			super(source);
		}
	}

}
