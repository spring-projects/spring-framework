package org.springframework.config.java.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

import java.lang.reflect.Field;
import java.util.Vector;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.config.java.Configuration;
import org.springframework.config.java.ext.Bean;
import org.springframework.util.ClassUtils;

/**
 * Unit tests for {@link ConfigurationPostProcessor}
 *
 * @author Chris Beams
 */
public class ConfigurationPostProcessorTests {

	private static final String ORIG_CGLIB_TEST_CLASS = ConfigurationPostProcessor.CGLIB_TEST_CLASS;
	private static final String BOGUS_CGLIB_TEST_CLASS = "a.bogus.class";

	/**
	 * CGLIB is an optional dependency for Core Spring.  If users attempt
	 * to use {@link Configuration} classes, they'll need it on the classpath;
	 * if Configuration classes are present in the bean factory and CGLIB
	 * is not present, an instructive exception should be thrown.
	 */
	@Test
	public void testFailFastIfCglibNotPresent() {
		@Configuration class Config {
			public @Bean String name() { return "foo"; }
		}

		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

		factory.registerBeanDefinition("config1", rootBeanDefinition(Config.class).getBeanDefinition());

		ConfigurationPostProcessor cpp = new ConfigurationPostProcessor();

		// temporarily set the cglib test class to something bogus
		ConfigurationPostProcessor.CGLIB_TEST_CLASS = BOGUS_CGLIB_TEST_CLASS;

		try {
			cpp.postProcessBeanFactory(factory);
		} catch (RuntimeException ex) {
			assertTrue(ex.getMessage().contains("CGLIB is required to process @Configuration classes"));
		} finally {
			ConfigurationPostProcessor.CGLIB_TEST_CLASS = ORIG_CGLIB_TEST_CLASS;
		}
	}

	/**
	 * In order to keep Spring's footprint as small as possible, CGLIB must
	 * not be required on the classpath unless the user is taking advantage
	 * of {@link Configuration} classes.
	 * 
	 * This test will fail if any CGLIB classes are classloaded before the call
	 * to {@link ConfigurationPostProcessor#enhanceConfigurationClasses}
	 */
	@Test
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
	 * Technically, {@link ConfigurationPostProcessor} could fail to enhance the
	 * registered Configuration classes, and many use cases would still work.
	 * Certain cases, however, like inter-bean singleton references would not.
	 * We test for such a case below, and in doing so prove that enhancement is
	 * working.
	 */
	@Test
	public void testEnhancementIsPresentBecauseSingletonSemanticsAreRespected() {
		DefaultListableBeanFactory beanFactory = new  DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("config",
				rootBeanDefinition(SingletonBeanConfig.class).getBeanDefinition());
		new ConfigurationPostProcessor().postProcessBeanFactory(beanFactory);
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

}
