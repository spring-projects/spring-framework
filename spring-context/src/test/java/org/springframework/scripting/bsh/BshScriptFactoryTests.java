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

package org.springframework.scripting.bsh;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.ConfigurableMessenger;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.TestBeanAwareMessenger;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BshScriptFactoryTests {

	@Test
	public void staticScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());

		assertThat(Arrays.asList(ctx.getBeanNamesForType(Calculator.class)).contains("calculator")).isTrue();
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messenger")).isTrue();

		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		boolean condition3 = calc instanceof Refreshable;
		assertThat(condition3).as("Scripted object should not be instance of Refreshable").isFalse();
		boolean condition2 = messenger instanceof Refreshable;
		assertThat(condition2).as("Scripted object should not be instance of Refreshable").isFalse();

		assertThat(calc).isEqualTo(calc);
		assertThat(messenger).isEqualTo(messenger);
		boolean condition1 = !messenger.equals(calc);
		assertThat(condition1).isTrue();
		assertThat(messenger.hashCode() != calc.hashCode()).isTrue();
		boolean condition = !messenger.toString().equals(calc.toString());
		assertThat(condition).isTrue();

		assertThat(calc.add(2, 3)).isEqualTo(5);

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

		assertThat(ctx.getBeansOfType(Calculator.class).values().contains(calc)).isTrue();
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();
	}

	@Test
	public void staticScriptWithNullReturnValue() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfig")).isTrue();

		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfig");
		messenger.setMessage(null);
		assertThat(messenger.getMessage()).isNull();
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();
	}

	@Test
	public void staticScriptWithTwoInterfacesSpecified() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfigExtra")).isTrue();

		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfigExtra");
		messenger.setMessage(null);
		assertThat(messenger.getMessage()).isNull();
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

		ctx.close();
		assertThat(messenger.getMessage()).isNull();
	}

	@Test
	public void staticWithScriptReturningInstance() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerInstance")).isTrue();

		Messenger messenger = (Messenger) ctx.getBean("messengerInstance");
		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

		ctx.close();
		assertThat(messenger.getMessage()).isNull();
	}

	@Test
	public void staticScriptImplementingInterface() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerImpl")).isTrue();

		Messenger messenger = (Messenger) ctx.getBean("messengerImpl");
		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
		assertThat(ctx.getBeansOfType(Messenger.class).values().contains(messenger)).isTrue();

		ctx.close();
		assertThat(messenger.getMessage()).isNull();
	}

	@Test
	public void staticPrototypeScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Scripted object should not be instance of Refreshable").isFalse();

		assertThat(messenger2).isNotSameAs(messenger);
		assertThat(messenger2.getClass()).isSameAs(messenger.getClass());
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		assertThat(messenger2.getMessage()).isEqualTo("Hello World!");
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertThat(messenger.getMessage()).isEqualTo("Bye World!");
		assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");
	}

	@Test
	public void nonStaticScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Should be an instance of Refreshable").isTrue();

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertThat(messenger.getMessage()).as("Message is incorrect after refresh").isEqualTo(desiredMessage);
		assertThat(refreshable.getRefreshCount()).as("Incorrect refresh count").isEqualTo(2);
	}

	@Test
	public void nonStaticPrototypeScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Should be an instance of Refreshable").isTrue();

		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		assertThat(messenger2.getMessage()).isEqualTo("Hello World!");
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertThat(messenger.getMessage()).isEqualTo("Bye World!");
		assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");
		assertThat(refreshable.getRefreshCount()).as("Incorrect refresh count").isEqualTo(2);
	}

	@Test
	public void scriptCompilationException() {
		assertThatExceptionOfType(NestedRuntimeException.class).isThrownBy(() ->
				new ClassPathXmlApplicationContext("org/springframework/scripting/bsh/bshBrokenContext.xml"))
			.matches(ex -> ex.contains(ScriptCompilationException.class));
	}

	@Test
	public void scriptThatCompilesButIsJustPlainBad() throws IOException {
		ScriptSource script = mock(ScriptSource.class);
		final String badScript = "String getMessage() { throw new IllegalArgumentException(); }";
		given(script.getScriptAsString()).willReturn(badScript);
		given(script.isModified()).willReturn(true);
		BshScriptFactory factory = new BshScriptFactory(
				ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript, Messenger.class);
		assertThatExceptionOfType(BshScriptUtils.BshExecutionException.class).isThrownBy(() -> {
			Messenger messenger = (Messenger) factory.getScriptedObject(script, Messenger.class);
			messenger.getMessage();
		});
	}

	@Test
	public void ctorWithNullScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BshScriptFactory(null, Messenger.class));
	}

	@Test
	public void ctorWithEmptyScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BshScriptFactory("", Messenger.class));
	}

	@Test
	public void ctorWithWhitespacedScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new BshScriptFactory("\n   ", Messenger.class));
	}

	@Test
	public void resourceScriptFromTag() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		TestBean testBean = (TestBean) ctx.getBean("testBean");

		Collection<String> beanNames = Arrays.asList(ctx.getBeanNamesForType(Messenger.class));
		assertThat(beanNames.contains("messenger")).isTrue();
		assertThat(beanNames.contains("messengerImpl")).isTrue();
		assertThat(beanNames.contains("messengerInstance")).isTrue();

		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).isFalse();

		Messenger messengerImpl = (Messenger) ctx.getBean("messengerImpl");
		assertThat(messengerImpl.getMessage()).isEqualTo("Hello World!");

		Messenger messengerInstance = (Messenger) ctx.getBean("messengerInstance");
		assertThat(messengerInstance.getMessage()).isEqualTo("Hello World!");

		TestBeanAwareMessenger messengerByType = (TestBeanAwareMessenger) ctx.getBean("messengerByType");
		assertThat(messengerByType.getTestBean()).isEqualTo(testBean);

		TestBeanAwareMessenger messengerByName = (TestBeanAwareMessenger) ctx.getBean("messengerByName");
		assertThat(messengerByName.getTestBean()).isEqualTo(testBean);

		Collection<Messenger> beans = ctx.getBeansOfType(Messenger.class).values();
		assertThat(beans.contains(messenger)).isTrue();
		assertThat(beans.contains(messengerImpl)).isTrue();
		assertThat(beans.contains(messengerInstance)).isTrue();
		assertThat(beans.contains(messengerByType)).isTrue();
		assertThat(beans.contains(messengerByName)).isTrue();

		ctx.close();
		assertThat(messenger.getMessage()).isNull();
		assertThat(messengerImpl.getMessage()).isNull();
		assertThat(messengerInstance.getMessage()).isNull();
	}

	@Test
	public void prototypeScriptFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(messenger2).isNotSameAs(messenger);
		assertThat(messenger2.getClass()).isSameAs(messenger.getClass());
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		assertThat(messenger2.getMessage()).isEqualTo("Hello World!");
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertThat(messenger.getMessage()).isEqualTo("Bye World!");
		assertThat(messenger2.getMessage()).isEqualTo("Byebye World!");
	}

	@Test
	public void inlineScriptFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertThat(calculator).isNotNull();
		boolean condition = calculator instanceof Refreshable;
		assertThat(condition).isFalse();
	}

	@Test
	public void refreshableFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		boolean condition = messenger instanceof Refreshable;
		assertThat(condition).as("Messenger should be Refreshable").isTrue();
	}

	@Test
	public void applicationEventListener() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Messenger eventListener = (Messenger) ctx.getBean("eventListener");
		ctx.publishEvent(new MyEvent(ctx));
		assertThat(eventListener.getMessage()).isEqualTo("count=2");
	}


	@SuppressWarnings("serial")
	private static class MyEvent extends ApplicationEvent {

		public MyEvent(Object source) {
			super(source);
		}
	}

}
