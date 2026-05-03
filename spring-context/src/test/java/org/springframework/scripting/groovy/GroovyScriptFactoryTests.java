/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.scripting.groovy;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Map;

import groovy.lang.DelegatingMetaClass;
import groovy.lang.GroovyObject;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.CallCounter;
import org.springframework.scripting.ConfigurableMessenger;
import org.springframework.scripting.ContextScriptBean;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Chris Beams
 */
@SuppressWarnings("resource")
class GroovyScriptFactoryTests {

	@Test
	void staticScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());

		assertThat(Arrays.asList(ctx.getBeanNamesForType(Calculator.class))).contains("calculator");
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messenger");

		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertThat(AopUtils.isAopProxy(calc)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();

		assertThat(calc).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

		assertThat(calc).isEqualTo(calc);
		assertThat(messenger).isEqualTo(messenger);
		assertThat(messenger).isNotEqualTo(calc);
		assertThat(messenger.hashCode()).isNotEqualTo(calc.hashCode());
		assertThat(messenger.toString()).isNotEqualTo(calc.toString());

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

		assertThat(ctx.getBeansOfType(Calculator.class)).containsValue(calc);
		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test
	void staticScriptUsingJsr223() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContextWithJsr223.xml", getClass());

		assertThat(Arrays.asList(ctx.getBeanNamesForType(Calculator.class))).contains("calculator");
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messenger");

		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertThat(AopUtils.isAopProxy(calc)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();

		assertThat(calc).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

		assertThat(calc).isEqualTo(calc);
		assertThat(messenger).isEqualTo(messenger);
		assertThat(messenger).isNotEqualTo(calc);
		assertThat(messenger.hashCode()).isNotEqualTo(calc.hashCode());
		assertThat(messenger.toString()).isNotEqualTo(calc.toString());

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

		assertThat(ctx.getBeansOfType(Calculator.class)).containsValue(calc);
		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test
	void staticPrototypeScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

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
	void staticPrototypeScriptUsingJsr223() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContextWithJsr223.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

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
	void staticScriptWithInstance() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messengerInstance");
		Messenger messenger = (Messenger) ctx.getBean("messengerInstance");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test
	void staticScriptWithInstanceUsingJsr223() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContextWithJsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messengerInstance");
		Messenger messenger = (Messenger) ctx.getBean("messengerInstance");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test
	void staticScriptWithInlineDefinedInstance() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messengerInstanceInline");
		Messenger messenger = (Messenger) ctx.getBean("messengerInstanceInline");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test
	void staticScriptWithInlineDefinedInstanceUsingJsr223() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContextWithJsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messengerInstanceInline");
		Messenger messenger = (Messenger) ctx.getBean("messengerInstanceInline");

		assertThat(AopUtils.isAopProxy(messenger)).as("Shouldn't get proxy when refresh is disabled").isFalse();
		assertThat(messenger).as("Scripted object should not be instance of Refreshable").isNotInstanceOf(Refreshable.class);

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);
		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test
	void nonStaticScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyRefreshableContext.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
		assertThat(messenger).as("Should be an instance of Refreshable").isInstanceOf(Refreshable.class);

		String desiredMessage = "Hello World!";
		assertThat(messenger.getMessage()).as("Message is incorrect").isEqualTo(desiredMessage);

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertThat(messenger.getMessage()).as("Message is incorrect after refresh.").isEqualTo(desiredMessage);
		assertThat(refreshable.getRefreshCount()).as("Incorrect refresh count").isEqualTo(2);
	}

	@Test
	void nonStaticPrototypeScript() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyRefreshableContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertThat(AopUtils.isAopProxy(messenger)).as("Should be a proxy for refreshable scripts").isTrue();
		assertThat(messenger).as("Should be an instance of Refreshable").isInstanceOf(Refreshable.class);

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
	void scriptCompilationException() {
		assertThatExceptionOfType(NestedRuntimeException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("org/springframework/scripting/groovy/groovyBrokenContext.xml"))
				.matches(ex -> ex.contains(ScriptCompilationException.class));
	}

	@Test
	void scriptedClassThatDoesNotHaveANoArgCtor() throws Exception {
		ScriptSource script = mock();
		String badScript = "class Foo { public Foo(String foo) {}}";
		given(script.getScriptAsString()).willReturn(badScript);
		given(script.suggestedClassName()).willReturn("someName");
		GroovyScriptFactory factory = new GroovyScriptFactory(ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript);
		assertThatExceptionOfType(ScriptCompilationException.class)
				.isThrownBy(() -> factory.getScriptedObject(script))
				.matches(ex -> ex.contains(NoSuchMethodException.class));
	}

	@Test
	void scriptedClassThatHasNoPublicNoArgCtor() throws Exception {
		ScriptSource script = mock();
		String badScript = "class Foo { protected Foo() {} \n String toString() { 'X' }}";
		given(script.getScriptAsString()).willReturn(badScript);
		given(script.suggestedClassName()).willReturn("someName");
		GroovyScriptFactory factory = new GroovyScriptFactory(ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript);
		assertThat(factory.getScriptedObject(script).toString()).isEqualTo("X");
	}

	@Test
	void withTwoClassesDefinedInTheOneGroovyFile_CorrectClassFirst() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("twoClassesCorrectOneFirst.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertThat(messenger).isNotNull();
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");

		// Check can cast to GroovyObject
		GroovyObject goo = (GroovyObject) messenger;
		assertThat(goo).isNotNull();
	}

	@Test
	void withTwoClassesDefinedInTheOneGroovyFile_WrongClassFirst() {
		assertThatException().as("two classes defined in GroovyScriptFactory source, non-Messenger class defined first").isThrownBy(() -> {
				ApplicationContext ctx = new ClassPathXmlApplicationContext("twoClassesWrongOneFirst.xml", getClass());
				ctx.getBean("messenger", Messenger.class);
		});
	}

	@Test
	void ctorWithNullScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GroovyScriptFactory(null));
	}

	@Test
	void ctorWithEmptyScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GroovyScriptFactory(""));
	}

	@Test
	void ctorWithWhitespacedScriptSourceLocator() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GroovyScriptFactory("\n   "));
	}

	@Test
	void withInlineScriptWithLeadingWhitespace() {
		assertThatExceptionOfType(BeanCreationException.class).as("'inline:' prefix was preceded by whitespace")
				.isThrownBy(() -> new ClassPathXmlApplicationContext("lwspBadGroovyContext.xml", getClass()))
				.matches(ex -> ex.contains(FileNotFoundException.class));
	}

	@Test
	void getScriptedObjectDoesNotChokeOnNullInterfacesBeingPassedIn() throws Exception {
		ScriptSource script = mock();
		given(script.getScriptAsString()).willReturn("class Bar {}");
		given(script.suggestedClassName()).willReturn("someName");

		GroovyScriptFactory factory = new GroovyScriptFactory("a script source locator (doesn't matter here)");
		Object scriptedObject = factory.getScriptedObject(script);
		assertThat(scriptedObject).isNotNull();
	}

	@Test
	void getScriptedObjectDoesChokeOnNullScriptSourceBeingPassedIn() {
		GroovyScriptFactory factory = new GroovyScriptFactory("a script source locator (doesn't matter here)");
		assertThatNullPointerException().as("NullPointerException as per contract ('null' ScriptSource supplied)")
				.isThrownBy(() -> factory.getScriptedObject(null));
	}

	@Test
	void resourceScriptFromTag() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		CallCounter countingAspect = (CallCounter) ctx.getBean("getMessageAspect");

		assertThat(AopUtils.isAopProxy(messenger)).isTrue();
		assertThat(messenger).isNotInstanceOf(Refreshable.class);
		assertThat(countingAspect.getCalls()).isEqualTo(0);
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		assertThat(countingAspect.getCalls()).isEqualTo(1);

		ctx.close();
		assertThat(countingAspect.getCalls()).isEqualTo(-200);
	}

	@Test
	void prototypeScriptFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
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
	void inlineScriptFromTag() {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		BeanDefinition bd = ctx.getBeanFactory().getBeanDefinition("calculator");
		assertThat(ObjectUtils.containsElement(bd.getDependsOn(), "messenger")).isTrue();
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertThat(calculator).isNotNull();
		assertThat(calculator).isNotInstanceOf(Refreshable.class);
	}

	@Test
	void refreshableFromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("refreshableMessenger");

		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		CallCounter countingAspect = (CallCounter) ctx.getBean("getMessageAspect");

		assertThat(AopUtils.isAopProxy(messenger)).isTrue();
		assertThat(messenger).isInstanceOf(Refreshable.class);
		assertThat(countingAspect.getCalls()).isEqualTo(0);
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
		assertThat(countingAspect.getCalls()).isEqualTo(1);

		assertThat(ctx.getBeansOfType(Messenger.class)).containsValue(messenger);
	}

	@Test  // SPR-6268
	void refreshableFromTagProxyTargetClass() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd-proxy-target-class.xml",
				getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("refreshableMessenger");

		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");

		assertThat(AopUtils.isAopProxy(messenger)).isTrue();
		assertThat(messenger).isInstanceOf(Refreshable.class);
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");

		assertThat(ctx.getBeansOfType(ConcreteMessenger.class)).containsValue((ConcreteMessenger) messenger);

		// Check that AnnotationUtils works with concrete proxied script classes
		assertThat(AnnotationUtils.findAnnotation(messenger.getClass(), Component.class)).isNotNull();
	}

	@Test  // SPR-6268
	void proxyTargetClassNotAllowedIfNotGroovy() {
		try {
			new ClassPathXmlApplicationContext("groovy-with-xsd-proxy-target-class.xml", getClass());
		}
		catch (BeanCreationException ex) {
			assertThat(ex.getMessage()).contains("Cannot use proxyTargetClass=true");
		}
	}

	@Test
	void anonymousScriptDetected() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		Map<?, Messenger> beans = ctx.getBeansOfType(Messenger.class);
		assertThat(beans).hasSize(4);
		assertThat(ctx.getBean(MyBytecodeProcessor.class).processed.contains(
				"org.springframework.scripting.groovy.GroovyMessenger2")).isTrue();
	}

	@Test
	void jsr223FromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd-jsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messenger");
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertThat(AopUtils.isAopProxy(messenger)).isFalse();
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
	}

	@Test
	void jsr223FromTagWithInterface() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd-jsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("messengerWithInterface");
		Messenger messenger = (Messenger) ctx.getBean("messengerWithInterface");
		assertThat(AopUtils.isAopProxy(messenger)).isFalse();
	}

	@Test
	void refreshableJsr223FromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd-jsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("refreshableMessenger");
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		assertThat(AopUtils.isAopProxy(messenger)).isTrue();
		assertThat(messenger).isInstanceOf(Refreshable.class);
		assertThat(messenger.getMessage()).isEqualTo("Hello World!");
	}

	@Test
	void inlineJsr223FromTag() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd-jsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("inlineMessenger");
		Messenger messenger = (Messenger) ctx.getBean("inlineMessenger");
		assertThat(AopUtils.isAopProxy(messenger)).isFalse();
	}

	@Test
	void inlineJsr223FromTagWithInterface() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd-jsr223.xml", getClass());
		assertThat(Arrays.asList(ctx.getBeanNamesForType(Messenger.class))).contains("inlineMessengerWithInterface");
		Messenger messenger = (Messenger) ctx.getBean("inlineMessengerWithInterface");
		assertThat(AopUtils.isAopProxy(messenger)).isFalse();
	}

	/**
	 * Tests the SPR-2098 bug whereby no more than 1 property element could be
	 * passed to a scripted bean :(
	 */
	@Test
	void canPassInMoreThanOneProperty() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-multiple-properties.xml", getClass());
		TestBean tb = (TestBean) ctx.getBean("testBean");

		ContextScriptBean bean = (ContextScriptBean) ctx.getBean("bean");
		assertThat(bean.getName()).as("The first property ain't bein' injected.").isEqualTo("Sophie Marceau");
		assertThat(bean.getAge()).as("The second property ain't bein' injected.").isEqualTo(31);
		assertThat(bean.getTestBean()).isEqualTo(tb);
		assertThat(bean.getApplicationContext()).isEqualTo(ctx);

		ContextScriptBean bean2 = (ContextScriptBean) ctx.getBean("bean2");
		assertThat(bean2.getTestBean()).isEqualTo(tb);
		assertThat(bean2.getApplicationContext()).isEqualTo(ctx);
	}

	@Test
	void metaClassWithBeans() {
		testMetaClass("org/springframework/scripting/groovy/calculators.xml");
	}

	@Test
	void metaClassWithXsd() {
		testMetaClass("org/springframework/scripting/groovy/calculators-with-xsd.xml");
	}

	private void testMetaClass(String xmlFile) {
		// expect the exception we threw in the custom metaclass to show it got invoked
		ApplicationContext ctx = new ClassPathXmlApplicationContext(xmlFile);
		Calculator calc = (Calculator) ctx.getBean("delegatingCalculator");
		assertThatIllegalStateException()
				.isThrownBy(() -> calc.add(1, 2))
				.withMessage("Gotcha");
	}

	@Test
	void factoryBean() {
		ApplicationContext context = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		Object factory = context.getBean("&factory");
		assertThat(factory).isInstanceOf(FactoryBean.class);
		Object result = context.getBean("factory");
		assertThat(result).isEqualTo("test");
	}

	@Test
	void refreshableFactoryBean() {
		ApplicationContext context = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		Object factory = context.getBean("&refreshableFactory");
		assertThat(factory).isInstanceOf(FactoryBean.class);
		Object result = context.getBean("refreshableFactory");
		assertThat(result).isEqualTo("test");
	}


	public static class TestCustomizer implements GroovyObjectCustomizer {

		@Override
		public void customize(GroovyObject goo) {
			DelegatingMetaClass dmc = new DelegatingMetaClass(goo.getMetaClass()) {
				@Override
				public Object invokeMethod(Object arg0, String mName, Object[] arg2) {
					if (mName.contains("Missing")) {
						throw new IllegalStateException("Gotcha");
					}
					else {
						return super.invokeMethod(arg0, mName, arg2);
					}
				}
			};
			dmc.initialize();
			goo.setMetaClass(dmc);
		}
	}

}
