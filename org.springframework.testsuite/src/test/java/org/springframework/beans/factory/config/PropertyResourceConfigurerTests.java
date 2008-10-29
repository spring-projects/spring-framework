/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;

import junit.framework.TestCase;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.IndexedTestBean;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.JdkVersion;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * @author Juergen Hoeller
 * @since 02.10.2003
 */
public class PropertyResourceConfigurerTests extends TestCase {

	public void testPropertyOverrideConfigurer() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb1.age=99\ntb2.name=test");
		ac.registerSingleton("configurer1", PropertyOverrideConfigurer.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb2.age=99\ntb2.name=test2");
		pvs.addPropertyValue("order", "0");
		ac.registerSingleton("configurer2", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		TestBean tb1 = (TestBean) ac.getBean("tb1");
		TestBean tb2 = (TestBean) ac.getBean("tb2");
		assertEquals(99, tb1.getAge());
		assertEquals(99, tb2.getAge());
		assertEquals(null, tb1.getName());
		assertEquals("test", tb2.getName());
	}

	public void testPropertyOverrideConfigurerWithNestedProperty() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb.array[0].age=99\ntb.list[1].name=test");
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	public void testPropertyOverrideConfigurerWithNestedPropertyAndDotInBeanName() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("my.tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "my.tb_array[0].age=99\nmy.tb_list[1].name=test");
		pvs.addPropertyValue("beanNameSeparator", "_");
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("my.tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	public void testPropertyOverrideConfigurerWithNestedMapPropertyAndDotInMapKey() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb.map[key1]=99\ntb.map[key2.ext]=test");
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("tb");
		assertEquals("99", tb.getMap().get("key1"));
		assertEquals("test", tb.getMap().get("key2.ext"));
	}

	public void testPropertyOverrideConfigurerWithJavaMailProperties() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", JavaMailSenderImpl.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb.javaMailProperties[mail.smtp.auth]=true");
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		JavaMailSenderImpl tb = (JavaMailSenderImpl) ac.getBean("tb");
		assertEquals("true", tb.getJavaMailProperties().getProperty("mail.smtp.auth"));
	}

	public void testPropertyOverrideConfigurerWithPropertiesFile() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("location", "classpath:org/springframework/beans/factory/config/test.properties");
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	public void testPropertyOverrideConfigurerWithInvalidPropertiesFile() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("locations",
				new String[] {"classpath:org/springframework/beans/factory/config/test.properties",
											"classpath:org/springframework/beans/factory/config/xtest.properties"});
		pvs.addPropertyValue("ignoreResourceNotFound", Boolean.TRUE);
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	public void testPropertyOverrideConfigurerWithPropertiesXmlFile() {
		// ignore for JDK < 1.5
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			return;
		}

		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("location", "classpath:org/springframework/beans/factory/config/test-properties.xml");
		ac.registerSingleton("configurer", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	public void testPropertyOverrideConfigurerWithConvertProperties() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb", IndexedTestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb.array[0].name=99\ntb.list[1].name=test");
		ac.registerSingleton("configurer", ConvertingOverrideConfigurer.class, pvs);
		ac.refresh();
		IndexedTestBean tb = (IndexedTestBean) ac.getBean("tb");
		assertEquals("X99", tb.getArray()[0].getName());
		assertEquals("Xtest", ((TestBean) tb.getList().get(1)).getName());
	}

	public void testPropertyOverrideConfigurerWithInvalidKey() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "argh=hgra\ntb1.age=99\ntb2.name=test");
		pvs.addPropertyValue("ignoreInvalidKeys", "true");
		ac.registerSingleton("configurer1", PropertyOverrideConfigurer.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb2.age=99\ntb2.name=test2");
		pvs.addPropertyValue("order", "0");
		ac.registerSingleton("configurer2", PropertyOverrideConfigurer.class, pvs);
		try {
			ac.refresh();
		}
		catch (BeanInitializationException ex) {
			assertTrue(ex.getMessage().toLowerCase().indexOf("argh") != -1);
		}
	}

	public void testPropertyOverrideConfigurerWithIgnoreInvalidKeys() {
		StaticApplicationContext ac = new StaticApplicationContext();
		ac.registerSingleton("tb1", TestBean.class);
		ac.registerSingleton("tb2", TestBean.class);
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "argh=hgra\ntb1.age=99\ntb2.name=test");
		pvs.addPropertyValue("ignoreInvalidKeys", "true");
		ac.registerSingleton("configurer1", PropertyOverrideConfigurer.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "tb2.age=99\ntb2.name=test2");
		pvs.addPropertyValue("order", "0");
		ac.registerSingleton("configurer2", PropertyOverrideConfigurer.class, pvs);
		ac.refresh();
		TestBean tb1 = (TestBean) ac.getBean("tb1");
		TestBean tb2 = (TestBean) ac.getBean("tb2");
		assertEquals(99, tb1.getAge());
		assertEquals(99, tb2.getAge());
		assertEquals(null, tb1.getName());
		assertEquals("test", tb2.getName());
	}

	public void testPropertyPlaceholderConfigurer() {
		doTestPropertyPlaceholderConfigurer(false);
	}

	public void testPropertyPlaceholderConfigurerWithParentChildSeparation() {
		doTestPropertyPlaceholderConfigurer(true);
	}

	private void doTestPropertyPlaceholderConfigurer(boolean parentChildSeparation) {
		StaticApplicationContext ac = new StaticApplicationContext();

		if (parentChildSeparation) {
			MutablePropertyValues pvs1 = new MutablePropertyValues();
			pvs1.addPropertyValue("age", "${age}");
			MutablePropertyValues pvs2 = new MutablePropertyValues();
			pvs2.addPropertyValue("name", "name${var}${var}${");
			pvs2.addPropertyValue("spouse", new RuntimeBeanReference("${ref}"));

			RootBeanDefinition parent = new RootBeanDefinition(TestBean.class, pvs1);
			ChildBeanDefinition bd = new ChildBeanDefinition("${parent}", pvs2);
			ac.getDefaultListableBeanFactory().registerBeanDefinition("parent1", parent);
			ac.getDefaultListableBeanFactory().registerBeanDefinition("tb1", bd);
		}
		else {
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.addPropertyValue("age", "${age}");
			pvs.addPropertyValue("name", "name${var}${var}${");
			pvs.addPropertyValue("spouse", new RuntimeBeanReference("${ref}"));
			RootBeanDefinition bd = new RootBeanDefinition(TestBean.class, pvs);
			ac.getDefaultListableBeanFactory().registerBeanDefinition("tb1", bd);
		}

		ConstructorArgumentValues cas = new ConstructorArgumentValues();
		cas.addIndexedArgumentValue(1, "${age}");
		cas.addGenericArgumentValue("${var}name${age}");

		MutablePropertyValues pvs = new MutablePropertyValues();
		List friends = new ManagedList();
		friends.add("na${age}me");
		friends.add(new RuntimeBeanReference("${ref}"));
		pvs.addPropertyValue("friends", friends);

		Set someSet = new ManagedSet();
		someSet.add("na${age}me");
		someSet.add(new RuntimeBeanReference("${ref}"));
		someSet.add(new TypedStringValue("${age}", Integer.class));
		pvs.addPropertyValue("someSet", someSet);

		Map someMap = new ManagedMap();
		someMap.put(new TypedStringValue("key${age}"), new TypedStringValue("${age}"));
		someMap.put(new TypedStringValue("key${age}ref"), new RuntimeBeanReference("${ref}"));
		someMap.put("key1", new RuntimeBeanReference("${ref}"));
		someMap.put("key2", "${age}name");
		MutablePropertyValues innerPvs = new MutablePropertyValues();
		innerPvs.addPropertyValue("touchy", "${os.name}");
		someMap.put("key3", new RootBeanDefinition(TestBean.class, innerPvs));
		MutablePropertyValues innerPvs2 = new MutablePropertyValues(innerPvs);
		someMap.put("${key4}", new BeanDefinitionHolder(new ChildBeanDefinition("tb1", innerPvs2), "child"));
		pvs.addPropertyValue("someMap", someMap);

		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class, cas, pvs);
		ac.getDefaultListableBeanFactory().registerBeanDefinition("tb2", bd);

		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "age=98\nvar=${m}var\nref=tb2\nm=my\nkey4=mykey4\nparent=parent1");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();

		TestBean tb1 = (TestBean) ac.getBean("tb1");
		TestBean tb2 = (TestBean) ac.getBean("tb2");
		assertEquals(98, tb1.getAge());
		assertEquals(98, tb2.getAge());
		assertEquals("namemyvarmyvar${", tb1.getName());
		assertEquals("myvarname98", tb2.getName());
		assertEquals(tb2, tb1.getSpouse());
		assertEquals(2, tb2.getFriends().size());
		assertEquals("na98me", tb2.getFriends().iterator().next());
		assertEquals(tb2, tb2.getFriends().toArray()[1]);
		assertEquals(3, tb2.getSomeSet().size());
		assertTrue(tb2.getSomeSet().contains("na98me"));
		assertTrue(tb2.getSomeSet().contains(tb2));
		assertTrue(tb2.getSomeSet().contains(new Integer(98)));
		assertEquals(6, tb2.getSomeMap().size());
		assertEquals("98", tb2.getSomeMap().get("key98"));
		assertEquals(tb2, tb2.getSomeMap().get("key98ref"));
		assertEquals(tb2, tb2.getSomeMap().get("key1"));
		assertEquals("98name", tb2.getSomeMap().get("key2"));
		TestBean inner1 = (TestBean) tb2.getSomeMap().get("key3");
		TestBean inner2 = (TestBean) tb2.getSomeMap().get("mykey4");
		assertEquals(0, inner1.getAge());
		assertEquals(null, inner1.getName());
		assertEquals(System.getProperty("os.name"), inner1.getTouchy());
		assertEquals(98, inner2.getAge());
		assertEquals("namemyvarmyvar${", inner2.getName());
		assertEquals(System.getProperty("os.name"), inner2.getTouchy());
	}

	public void testPropertyPlaceholderConfigurerWithSystemPropertyFallback() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${os.name}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals(System.getProperty("os.name"), tb.getTouchy());
	}

	public void testPropertyPlaceholderConfigurerWithSystemPropertyNotUsed() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${os.name}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("os.name", "myos");
		pvs.addPropertyValue("properties", props);
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("myos", tb.getTouchy());
	}

	public void testPropertyPlaceholderConfigurerWithOverridingSystemProperty() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${os.name}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("os.name", "myos");
		pvs.addPropertyValue("properties", props);
		pvs.addPropertyValue("systemPropertiesModeName", "SYSTEM_PROPERTIES_MODE_OVERRIDE");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals(System.getProperty("os.name"), tb.getTouchy());
	}

	public void testPropertyPlaceholderConfigurerWithUnresolvableSystemProperty() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${user.dir}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("systemPropertiesModeName", "SYSTEM_PROPERTIES_MODE_NEVER");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getMessage().indexOf("user.dir") != -1);
		}
	}

	public void testPropertyPlaceholderConfigurerWithUnresolvablePlaceholder() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${ref}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, null);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getMessage().indexOf("ref") != -1);
		}
	}

	public void testPropertyPlaceholderConfigurerWithIgnoreUnresolvablePlaceholder() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${ref}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("ignoreUnresolvablePlaceholders", Boolean.TRUE);
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("${ref}", tb.getName());
	}

	public void testPropertyPlaceholderConfigurerWithEmptyStringAsNull() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("nullValue", "");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertNull(tb.getName());
	}

	public void testPropertyPlaceholderConfigurerWithEmptyStringInPlaceholderAsNull() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${ref}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("nullValue", "");
		Properties props = new Properties();
		props.put("ref", "");
		pvs.addPropertyValue("properties", props);
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertNull(tb.getName());
	}

	public void testPropertyPlaceholderConfigurerWithNestedPlaceholderInKey() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${my${key}key}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("key", "new");
		props.put("mynewkey", "myname");
		pvs.addPropertyValue("properties", props);
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("myname", tb.getName());
	}

	public void testPropertyPlaceholderConfigurerWithSystemPropertyInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("location", "${user.dir}/test");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanInitializationException");
		}
		catch (BeanInitializationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof FileNotFoundException);
			// slight hack for Linux/Unix systems
			String userDir = StringUtils.cleanPath(System.getProperty("user.dir"));
			if (userDir.startsWith("/")) {
				userDir = userDir.substring(1);
			}
			assertTrue(ex.getMessage().indexOf(userDir) != -1);
		}
	}

	public void testPropertyPlaceholderConfigurerWithSystemPropertiesInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("location", "${user.dir}/test/${user.dir}");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanInitializationException");
		}
		catch (BeanInitializationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof FileNotFoundException);
			// slight hack for Linux/Unix systems			
			String userDir = StringUtils.cleanPath(System.getProperty("user.dir"));
			if (userDir.startsWith("/")) {
				userDir = userDir.substring(1);
			}
			/* the above hack doesn't work since the exception message is created without
			   the leading / stripped so the test fails.  Changed 17/11/04. DD */
			//assertTrue(ex.getMessage().indexOf(userDir + "/test/" + userDir) != -1);		
			assertTrue(ex.getMessage().indexOf(userDir + "/test/" + userDir) != -1 ||
			    ex.getMessage().indexOf(userDir + "/test//" + userDir) != -1);
		}
	}

	public void testPropertyPlaceholderConfigurerWithUnresolvableSystemPropertiesInLocation() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("spouse", new RuntimeBeanReference("${ref}"));
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("location", "${myprop}/test/${myprop}");
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanInitializationException ex) {
			// expected
			assertTrue(ex.getCause() instanceof FileNotFoundException);
			assertTrue(ex.getMessage().indexOf("myprop") != -1);
		}
	}

	public void testPropertyPlaceholderConfigurerWithCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("age", "${age}");
		pvs.addPropertyValue("name", "name${var}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "age=99\nvar=${m}var\nm=${var}");
		ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	public void testPropertyPlaceholderConfigurerWithMultiLevelCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "var=${m}var\nm=${var2}\nvar2=${var}");
		ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	public void testPropertyPlaceholderConfigurerWithNestedCircularReference() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "name${var}");
		ac.registerSingleton("tb1", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("properties", "var=${m}var\nm=${var2}\nvar2=${m}");
		ac.registerSingleton("configurer1", PropertyPlaceholderConfigurer.class, pvs);
		try {
			ac.refresh();
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	public void testPropertyPlaceholderConfigurerWithDefaultProperties() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${test}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("test", "mytest");
		pvs.addPropertyValue("properties", new Properties(props));
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
	}

	public void testPropertyPlaceholderConfigurerWithAutowireByType() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${test}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		pvs.addPropertyValue("target", new RuntimeBeanReference("tb"));
		ac.registerSingleton("tbProxy", ProxyFactoryBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("test", "mytest");
		pvs.addPropertyValue("properties", new Properties(props));
		RootBeanDefinition ppcDef = new RootBeanDefinition(PropertyPlaceholderConfigurer.class, pvs);
		ppcDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_BY_TYPE);
		ac.registerBeanDefinition("configurer", ppcDef);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
	}

	public void testPropertyPlaceholderConfigurerWithAliases() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("touchy", "${test}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		ac.registerAlias("tb", "${myAlias}");
		ac.registerAlias("${myTarget}", "alias2");
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("test", "mytest");
		props.put("myAlias", "alias");
		props.put("myTarget", "tb");
		pvs.addPropertyValue("properties", new Properties(props));
		ac.registerSingleton("configurer", PropertyPlaceholderConfigurer.class, pvs);
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
		tb = (TestBean) ac.getBean("alias");
		assertEquals("mytest", tb.getTouchy());
		tb = (TestBean) ac.getBean("alias2");
		assertEquals("mytest", tb.getTouchy());
	}

	public void testPreferencesPlaceholderConfigurer() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${myName}");
		pvs.addPropertyValue("age", "${myAge}");
		pvs.addPropertyValue("touchy", "${myTouchy}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("myAge", "99");
		pvs.addPropertyValue("properties", props);
		ac.registerSingleton("configurer", PreferencesPlaceholderConfigurer.class, pvs);
		Preferences.systemRoot().put("myName", "myNameValue");
		Preferences.systemRoot().put("myTouchy", "myTouchyValue");
		Preferences.userRoot().put("myTouchy", "myOtherTouchyValue");
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("myNameValue", tb.getName());
		assertEquals(99, tb.getAge());
		assertEquals("myOtherTouchyValue", tb.getTouchy());
		Preferences.userRoot().remove("myTouchy");
		Preferences.systemRoot().remove("myTouchy");
		Preferences.systemRoot().remove("myName");
	}

	public void testPreferencesPlaceholderConfigurerWithCustomTreePaths() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${myName}");
		pvs.addPropertyValue("age", "${myAge}");
		pvs.addPropertyValue("touchy", "${myTouchy}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("myAge", "99");
		pvs.addPropertyValue("properties", props);
		pvs.addPropertyValue("systemTreePath", "mySystemPath");
		pvs.addPropertyValue("userTreePath", "myUserPath");
		ac.registerSingleton("configurer", PreferencesPlaceholderConfigurer.class, pvs);
		Preferences.systemRoot().node("mySystemPath").put("myName", "myNameValue");
		Preferences.systemRoot().node("mySystemPath").put("myTouchy", "myTouchyValue");
		Preferences.userRoot().node("myUserPath").put("myTouchy", "myOtherTouchyValue");
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("myNameValue", tb.getName());
		assertEquals(99, tb.getAge());
		assertEquals("myOtherTouchyValue", tb.getTouchy());
		Preferences.userRoot().node("myUserPath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath").remove("myName");
	}

	public void testPreferencesPlaceholderConfigurerWithPathInPlaceholder() {
		StaticApplicationContext ac = new StaticApplicationContext();
		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.addPropertyValue("name", "${mypath/myName}");
		pvs.addPropertyValue("age", "${myAge}");
		pvs.addPropertyValue("touchy", "${myotherpath/myTouchy}");
		ac.registerSingleton("tb", TestBean.class, pvs);
		pvs = new MutablePropertyValues();
		Properties props = new Properties();
		props.put("myAge", "99");
		pvs.addPropertyValue("properties", props);
		pvs.addPropertyValue("systemTreePath", "mySystemPath");
		pvs.addPropertyValue("userTreePath", "myUserPath");
		ac.registerSingleton("configurer", PreferencesPlaceholderConfigurer.class, pvs);
		Preferences.systemRoot().node("mySystemPath").node("mypath").put("myName", "myNameValue");
		Preferences.systemRoot().node("mySystemPath/myotherpath").put("myTouchy", "myTouchyValue");
		Preferences.userRoot().node("myUserPath/myotherpath").put("myTouchy", "myOtherTouchyValue");
		ac.refresh();
		TestBean tb = (TestBean) ac.getBean("tb");
		assertEquals("myNameValue", tb.getName());
		assertEquals(99, tb.getAge());
		assertEquals("myOtherTouchyValue", tb.getTouchy());
		Preferences.userRoot().node("myUserPath/myotherpath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath/myotherpath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath/mypath").remove("myName");
	}


	private static class ConvertingOverrideConfigurer extends PropertyOverrideConfigurer {

		protected String convertPropertyValue(String originalValue) {
			return "X" + originalValue;
		}
	}

}
