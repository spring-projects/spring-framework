/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.context.annotation;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import java.lang.reflect.Field;
import java.util.Vector;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.util.ClassUtils;


/**
 * Unit tests for {@link ConfigurationClassPostProcessor}.
 *
 * @author Chris Beams
 */
public class ConfigurationClassPostProcessorTests {

	private static final String ORIG_CGLIB_TEST_CLASS = ConfigurationClassPostProcessor.CGLIB_TEST_CLASS;
	private static final String BOGUS_CGLIB_TEST_CLASS = "a.bogus.class";

	/**
	 * CGLIB is an optional dependency for Spring.  If users attempt
	 * to use {@link Configuration} classes, they'll need it on the classpath;
	 * if Configuration classes are present in the bean factory and CGLIB
	 * is not present, an instructive exception should be thrown.
	 */
	@Test
	public void testFailFastIfCglibNotPresent() {
		@Configuration class Config { }

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("config", rootBeanDefinition(Config.class).getBeanDefinition());
		ConfigurationClassPostProcessor cpp = new ConfigurationClassPostProcessor();

		// temporarily set the cglib test class to something bogus
		ConfigurationClassPostProcessor.CGLIB_TEST_CLASS = BOGUS_CGLIB_TEST_CLASS;

		try {
			cpp.postProcessBeanFactory(factory);
		} catch (RuntimeException ex) {
			assertTrue(ex.getMessage().contains("CGLIB is required to process @Configuration classes"));
		} finally {
			ConfigurationClassPostProcessor.CGLIB_TEST_CLASS = ORIG_CGLIB_TEST_CLASS;
		}
	}

	/**
	 * In order to keep Spring's footprint as small as possible, CGLIB must
	 * not be required on the classpath unless the user is taking advantage
	 * of {@link Configuration} classes.
	 * 
	 * This test will fail if any CGLIB classes are classloaded before the call
	 * to {@link ConfigurationClassPostProcessor#enhanceConfigurationClasses}
	 */
	@Ignore @Test // because classloader hacking below causes extremely hard to
	              // debug downstream side effects. Re-enable at will to verify
	              // CGLIB is not prematurely classloaded, but this technique is
	              // not stable enough to leave enabled.
	public void testCglibClassesAreLoadedJustInTimeForEnhancement() throws Exception {
		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
		Field classesField = ClassLoader.class.getDeclaredField("classes");
		classesField.setAccessible(true);

		// first, remove any CGLIB classes that may have been loaded by other tests
		@SuppressWarnings("unchecked")
		Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

		Vector<Class<?>> cglibClassesAlreadyLoaded = new Vector<Class<?>>();
		for(Class<?> loadedClass : classes)
			if(loadedClass.getName().startsWith("net.sf.cglib"))
				cglibClassesAlreadyLoaded.add(loadedClass);

		for(Class<?> cglibClass : cglibClassesAlreadyLoaded)
			classes.remove(cglibClass);

		// now, execute a scenario where everything except enhancement occurs
		// -- no CGLIB classes should get loaded!
		testFailFastIfCglibNotPresent();

		// test to ensure that indeed no CGLIB classes have been loaded
		for(Class<?> loadedClass : classes)
			if(loadedClass.getName().startsWith("net.sf.cglib"))
				fail("CGLIB class should not have been eagerly loaded: " + loadedClass.getName());
	}

	/**
	 * Enhanced {@link Configuration} classes are only necessary for respecting
	 * certain bean semantics, like singleton-scoping, scoped proxies, etc.
	 * 
	 * Technically, {@link ConfigurationClassPostProcessor} could fail to enhance the
	 * registered Configuration classes and many use cases would still work.
	 * Certain cases, however, like inter-bean singleton references would not.
	 * We test for such a case below, and in doing so prove that enhancement is
	 * working.
	 */
	@Test
	public void testEnhancementIsPresentBecauseSingletonSemanticsAreRespected() {
		DefaultListableBeanFactory beanFactory = new  DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config",
				rootBeanDefinition(SingletonBeanConfig.class).getBeanDefinition());
		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
		Foo foo = (Foo) beanFactory.getBean("foo");
		Bar bar = (Bar) beanFactory.getBean("bar");
		assertThat(foo, sameInstance(bar.foo));
	}

	@Configuration
	static class SingletonBeanConfig {
		public @Bean Foo foo() {
			return new Foo();
		}

		public @Bean Bar bar() {
			return new Bar(foo());
		}
	}

	static class Foo { }
	static class Bar {
		final Foo foo;
		public Bar(Foo foo) { this.foo = foo; }
	}

	/**
	 * Tests the fix for SPR-5655, a special workaround that prefers reflection
	 * over ASM if a bean class is already loaded.
	 */
	@Test
	public void testAlreadyLoadedConfigurationClasses() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("unloadedConfig",
				rootBeanDefinition(UnloadedConfig.class.getName()).getBeanDefinition());
		beanFactory.registerBeanDefinition("loadedConfig",
				rootBeanDefinition(LoadedConfig.class).getBeanDefinition());
		new ConfigurationClassPostProcessor() .postProcessBeanFactory(beanFactory);
		beanFactory.getBean("foo");
		beanFactory.getBean("bar");
	}

	@Configuration
	static class UnloadedConfig {
		public @Bean Foo foo() {
			return new Foo();
		}
	}

	@Configuration
	static class LoadedConfig {
		public @Bean Bar bar() {
			return new Bar(new Foo());
		}
	}
}
