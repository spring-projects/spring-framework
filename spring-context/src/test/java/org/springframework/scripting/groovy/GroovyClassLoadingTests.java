/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import java.lang.reflect.Method;

import groovy.lang.GroovyClassLoader;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
class GroovyClassLoadingTests {

	@Test
	@SuppressWarnings("resource")
	void classLoading() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();

		GroovyClassLoader gcl = new GroovyClassLoader();
		Class<?> class1 = gcl.parseClass("class TestBean { def myMethod() { \"foo\" } }");
		Class<?> class2 = gcl.parseClass("class TestBean { def myMethod() { \"bar\" } }");

		context.registerBeanDefinition("testBean", new RootBeanDefinition(class1));
		Object testBean1 = context.getBean("testBean");
		Method method1 = class1.getDeclaredMethod("myMethod", new Class<?>[0]);
		Object result1 = ReflectionUtils.invokeMethod(method1, testBean1);
		assertThat(result1).isEqualTo("foo");

		context.removeBeanDefinition("testBean");
		context.registerBeanDefinition("testBean", new RootBeanDefinition(class2));
		Object testBean2 = context.getBean("testBean");
		Method method2 = class2.getDeclaredMethod("myMethod", new Class<?>[0]);
		Object result2 = ReflectionUtils.invokeMethod(method2, testBean2);
		assertThat(result2).isEqualTo("bar");
	}

}
