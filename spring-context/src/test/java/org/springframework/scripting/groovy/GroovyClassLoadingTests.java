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
	public void classLoading() throws Exception {
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
