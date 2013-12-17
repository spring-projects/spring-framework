/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.expression;

import java.io.Serializable;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.SerializationTestUtils;
import org.springframework.util.StopWatch;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ApplicationContextExpressionTests {

	private static final Log factoryLog = LogFactory.getLog(DefaultListableBeanFactory.class);

	@Test
	public void genericApplicationContext() throws Exception {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);

		ac.getBeanFactory().registerScope("myScope", new Scope() {
			@Override
			public Object get(String name, ObjectFactory<?> objectFactory) {
				return objectFactory.getObject();
			}
			@Override
			public Object remove(String name) {
				return null;
			}
			@Override
			public void registerDestructionCallback(String name, Runnable callback) {
			}
			@Override
			public Object resolveContextualObject(String key) {
				if (key.equals("mySpecialAttr")) {
					return "42";
				}
				else {
					return null;
				}
			}
			@Override
			public String getConversationId() {
				return null;
			}
		});

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties placeholders = new Properties();
		placeholders.setProperty("code", "123");
		ppc.setProperties(placeholders);
		ac.addBeanFactoryPostProcessor(ppc);

		GenericBeanDefinition bd0 = new GenericBeanDefinition();
		bd0.setBeanClass(TestBean.class);
		bd0.getPropertyValues().add("name", "myName");
		bd0.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "original"));
		ac.registerBeanDefinition("tb0", bd0);

		GenericBeanDefinition bd1 = new GenericBeanDefinition();
		bd1.setBeanClass(TestBean.class);
		bd1.setScope("myScope");
		bd1.getConstructorArgumentValues().addGenericArgumentValue("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ");
		bd1.getConstructorArgumentValues().addGenericArgumentValue("#{mySpecialAttr}");
		ac.registerBeanDefinition("tb1", bd1);

		GenericBeanDefinition bd2 = new GenericBeanDefinition();
		bd2.setBeanClass(TestBean.class);
		bd2.setScope("myScope");
		bd2.getPropertyValues().add("name", "{ XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ }");
		bd2.getPropertyValues().add("age", "#{mySpecialAttr}");
		bd2.getPropertyValues().add("country", "${code} #{systemProperties.country}");
		ac.registerBeanDefinition("tb2", bd2);

		GenericBeanDefinition bd3 = new GenericBeanDefinition();
		bd3.setBeanClass(ValueTestBean.class);
		bd3.setScope("myScope");
		ac.registerBeanDefinition("tb3", bd3);

		GenericBeanDefinition bd4 = new GenericBeanDefinition();
		bd4.setBeanClass(ConstructorValueTestBean.class);
		bd4.setScope("myScope");
		ac.registerBeanDefinition("tb4", bd4);

		GenericBeanDefinition bd5 = new GenericBeanDefinition();
		bd5.setBeanClass(MethodValueTestBean.class);
		bd5.setScope("myScope");
		ac.registerBeanDefinition("tb5", bd5);

		GenericBeanDefinition bd6 = new GenericBeanDefinition();
		bd6.setBeanClass(PropertyValueTestBean.class);
		bd6.setScope("myScope");
		ac.registerBeanDefinition("tb6", bd6);

		System.getProperties().put("country", "UK");
		try {
			ac.refresh();

			TestBean tb0 = ac.getBean("tb0", TestBean.class);

			TestBean tb1 = ac.getBean("tb1", TestBean.class);
			assertEquals("XXXmyNameYYY42ZZZ", tb1.getName());
			assertEquals(42, tb1.getAge());

			TestBean tb2 = ac.getBean("tb2", TestBean.class);
			assertEquals("{ XXXmyNameYYY42ZZZ }", tb2.getName());
			assertEquals(42, tb2.getAge());
			assertEquals("123 UK", tb2.getCountry());

			ValueTestBean tb3 = ac.getBean("tb3", ValueTestBean.class);
			assertEquals("XXXmyNameYYY42ZZZ", tb3.name);
			assertEquals(42, tb3.age);
			assertEquals("123 UK", tb3.country);
			assertEquals("123 UK", tb3.countryFactory.getObject());
			System.getProperties().put("country", "US");
			assertEquals("123 UK", tb3.country);
			assertEquals("123 US", tb3.countryFactory.getObject());
			System.getProperties().put("country", "UK");
			assertEquals("123 UK", tb3.country);
			assertEquals("123 UK", tb3.countryFactory.getObject());
			assertSame(tb0, tb3.tb);

			tb3 = (ValueTestBean) SerializationTestUtils.serializeAndDeserialize(tb3);
			assertEquals("123 UK", tb3.countryFactory.getObject());

			ConstructorValueTestBean tb4 = ac.getBean("tb4", ConstructorValueTestBean.class);
			assertEquals("XXXmyNameYYY42ZZZ", tb4.name);
			assertEquals(42, tb4.age);
			assertEquals("123 UK", tb4.country);
			assertSame(tb0, tb4.tb);

			MethodValueTestBean tb5 = ac.getBean("tb5", MethodValueTestBean.class);
			assertEquals("XXXmyNameYYY42ZZZ", tb5.name);
			assertEquals(42, tb5.age);
			assertEquals("123 UK", tb5.country);
			assertSame(tb0, tb5.tb);

			PropertyValueTestBean tb6 = ac.getBean("tb6", PropertyValueTestBean.class);
			assertEquals("XXXmyNameYYY42ZZZ", tb6.name);
			assertEquals(42, tb6.age);
			assertEquals("123 UK", tb6.country);
			assertSame(tb0, tb6.tb);
		}
		finally {
			System.getProperties().remove("country");
		}
	}

	@Test
	public void prototypeCreationReevaluatesExpressions() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
		GenericConversionService cs = new GenericConversionService();
		cs.addConverter(String.class, String.class, new Converter<String, String>() {
			@Override
			public String convert(String source) {
				return source.trim();
			}
		});
		ac.getBeanFactory().registerSingleton(GenericApplicationContext.CONVERSION_SERVICE_BEAN_NAME, cs);
		RootBeanDefinition rbd = new RootBeanDefinition(PrototypeTestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getPropertyValues().add("country", "#{systemProperties.country}");
		rbd.getPropertyValues().add("country2", new TypedStringValue("-#{systemProperties.country}-"));
		ac.registerBeanDefinition("test", rbd);
		ac.refresh();

		try {
			System.getProperties().put("name", "juergen1");
			System.getProperties().put("country", " UK1 ");
			PrototypeTestBean tb = (PrototypeTestBean) ac.getBean("test");
			assertEquals("juergen1", tb.getName());
			assertEquals("UK1", tb.getCountry());
			assertEquals("-UK1-", tb.getCountry2());

			System.getProperties().put("name", "juergen2");
			System.getProperties().put("country", "  UK2  ");
			tb = (PrototypeTestBean) ac.getBean("test");
			assertEquals("juergen2", tb.getName());
			assertEquals("UK2", tb.getCountry());
			assertEquals("-UK2-", tb.getCountry2());
		}
		finally {
			System.getProperties().remove("name");
			System.getProperties().remove("country");
		}
	}

	@Test
	public void prototypeCreationIsFastEnough() {
		Assume.group(TestGroup.PERFORMANCE);
		Assume.notLogging(factoryLog);
		GenericApplicationContext ac = new GenericApplicationContext();
		RootBeanDefinition rbd = new RootBeanDefinition(TestBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		rbd.getConstructorArgumentValues().addGenericArgumentValue("#{systemProperties.name}");
		rbd.getPropertyValues().add("country", "#{systemProperties.country}");
		ac.registerBeanDefinition("test", rbd);
		ac.refresh();
		StopWatch sw = new StopWatch();
		sw.start("prototype");
		System.getProperties().put("name", "juergen");
		System.getProperties().put("country", "UK");
		try {
			for (int i = 0; i < 100000; i++) {
				TestBean tb = (TestBean) ac.getBean("test");
				assertEquals("juergen", tb.getName());
				assertEquals("UK", tb.getCountry());
			}
			sw.stop();
		}
		finally {
			System.getProperties().remove("country");
			System.getProperties().remove("name");
		}
		assertTrue("Prototype creation took too long: " + sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 6000);
	}

	@Test
	public void systemPropertiesSecurityManager() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(TestBean.class);
		bd.getPropertyValues().add("country", "#{systemProperties.country}");
		ac.registerBeanDefinition("tb", bd);

		SecurityManager oldSecurityManager = System.getSecurityManager();
		try {
			System.setProperty("country", "NL");

			SecurityManager securityManager = new SecurityManager() {
				@Override
				public void checkPropertiesAccess() {
					throw new AccessControlException("Not Allowed");
				}
				@Override
				public void checkPermission(Permission perm) {
					// allow everything else
				}
			};
			System.setSecurityManager(securityManager);
			ac.refresh();

			TestBean tb = ac.getBean("tb", TestBean.class);
			assertEquals("NL", tb.getCountry());

		}
		finally {
			System.setSecurityManager(oldSecurityManager);
			System.getProperties().remove("country");
		}
	}

	@Test
	public void stringConcatenationWithDebugLogging() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(String.class);
		bd.getConstructorArgumentValues().addGenericArgumentValue("test-#{ T(java.lang.System).currentTimeMillis() }");
		ac.registerBeanDefinition("str", bd);
		ac.refresh();

		String str = ac.getBean("str", String.class);
		assertTrue(str.startsWith("test-"));
	}


	@SuppressWarnings("serial")
	public static class ValueTestBean implements Serializable {

		@Autowired @Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ")
		public String name;

		@Autowired @Value("#{mySpecialAttr}")
		public int age;

		@Value("${code} #{systemProperties.country}")
		public String country;

		@Value("${code} #{systemProperties.country}")
		public ObjectFactory<String> countryFactory;

		@Autowired @Qualifier("original")
		public transient TestBean tb;
	}


	public static class ConstructorValueTestBean {

		public String name;

		public int age;

		public String country;

		public TestBean tb;

		@Autowired
		public ConstructorValueTestBean(
				@Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ") String name,
				@Value("#{mySpecialAttr}") int age,
				@Qualifier("original") TestBean tb,
				@Value("${code} #{systemProperties.country}") String country) {
			this.name = name;
			this.age = age;
			this.country = country;
			this.tb = tb;
		}
	}


	public static class MethodValueTestBean {

		public String name;

		public int age;

		public String country;

		public TestBean tb;

		@Autowired
		public void configure(
				@Qualifier("original") TestBean tb,
				@Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ") String name,
				@Value("#{mySpecialAttr}") int age,
				@Value("${code} #{systemProperties.country}") String country) {
			this.name = name;
			this.age = age;
			this.country = country;
			this.tb = tb;
		}
	}


	public static class PropertyValueTestBean {

		public String name;

		public int age;

		public String country;

		public TestBean tb;

		@Value("XXX#{tb0.name}YYY#{mySpecialAttr}ZZZ")
		public void setName(String name) {
			this.name = name;
		}

		@Value("#{mySpecialAttr}")
		public void setAge(int age) {
			this.age = age;
		}

		@Value("${code} #{systemProperties.country}")
		public void setCountry(String country) {
			this.country = country;
		}

		@Autowired @Qualifier("original")
		public void setTb(TestBean tb) {
			this.tb = tb;
		}
	}


	public static class PrototypeTestBean {

		public String name;

		public String country;

		public String country2;

		@Value("#{systemProperties.name}")
		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry2(String country2) {
			this.country2 = country2;
		}

		public String getCountry2() {
			return country2;
		}
	}

}
