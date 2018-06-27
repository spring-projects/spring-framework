/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.IndexedTestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.StringUtils;

import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * Unit tests for various {@link PropertyResourceConfigurer} implementations including:
 * {@link PropertyPlaceholderConfigurer}, {@link PropertyOverrideConfigurer} and
 * {@link PreferencesPlaceholderConfigurer}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @since 02.10.2003
 * @see PropertyPlaceholderConfigurerTests
 */
public class PropertyResourceConfigurerTests {

	static {
		System.setProperty("java.util.prefs.PreferencesFactory", MockPreferencesFactory.class.getName());
	}

	private static final Class<?> CLASS = PropertyResourceConfigurerTests.class;
	private static final Resource TEST_PROPS = qualifiedResource(CLASS, "test.properties");
	private static final Resource XTEST_PROPS = qualifiedResource(CLASS, "xtest.properties"); // does not exist
	private static final Resource TEST_PROPS_XML = qualifiedResource(CLASS, "test.properties.xml");

	private final DefaultListableBeanFactory factory = new DefaultListableBeanFactory();


	@Test
	public void testPropertyOverrideConfigurer() {
		BeanDefinition def1 = BeanDefinitionBuilder.genericBeanDefinition(TestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb1", def1);

		BeanDefinition def2 = BeanDefinitionBuilder.genericBeanDefinition(TestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb2", def2);

		PropertyOverrideConfigurer poc1;
		PropertyOverrideConfigurer poc2;

		{
			poc1 = new PropertyOverrideConfigurer();
			Properties props = new Properties();
			props.setProperty("tb1.age", "99");
			props.setProperty("tb2.name", "test");
			poc1.setProperties(props);
		}

		{
			poc2 = new PropertyOverrideConfigurer();
			Properties props = new Properties();
			props.setProperty("tb2.age", "99");
			props.setProperty("tb2.name", "test2");
			poc2.setProperties(props);
		}

		// emulate what happens when BFPPs are added to an application context: It's LIFO-based
		poc2.postProcessBeanFactory(factory);
		poc1.postProcessBeanFactory(factory);

		TestBean tb1 = (TestBean) factory.getBean("tb1");
		TestBean tb2 = (TestBean) factory.getBean("tb2");

		assertEquals(99, tb1.getAge());
		assertEquals(99, tb2.getAge());
		assertEquals(null, tb1.getName());
		assertEquals("test", tb2.getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithNestedProperty() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc;
		poc = new PropertyOverrideConfigurer();
		Properties props = new Properties();
		props.setProperty("tb.array[0].age", "99");
		props.setProperty("tb.list[1].name", "test");
		poc.setProperties(props);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithNestedPropertyAndDotInBeanName() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("my.tb", def);

		PropertyOverrideConfigurer poc;
		poc = new PropertyOverrideConfigurer();
		Properties props = new Properties();
		props.setProperty("my.tb_array[0].age", "99");
		props.setProperty("my.tb_list[1].name", "test");
		poc.setProperties(props);
		poc.setBeanNameSeparator("_");
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("my.tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithNestedMapPropertyAndDotInMapKey() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc;
		poc = new PropertyOverrideConfigurer();
		Properties props = new Properties();
		props.setProperty("tb.map[key1]", "99");
		props.setProperty("tb.map[key2.ext]", "test");
		poc.setProperties(props);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertEquals("99", tb.getMap().get("key1"));
		assertEquals("test", tb.getMap().get("key2.ext"));
	}

	@Test
	public void testPropertyOverrideConfigurerWithHeldProperties() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(PropertiesHolder.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc;
		poc = new PropertyOverrideConfigurer();
		Properties props = new Properties();
		props.setProperty("tb.heldProperties[mail.smtp.auth]", "true");
		poc.setProperties(props);
		poc.postProcessBeanFactory(factory);

		PropertiesHolder tb = (PropertiesHolder) factory.getBean("tb");
		assertEquals("true", tb.getHeldProperties().getProperty("mail.smtp.auth"));
	}

	@Test
	public void testPropertyOverrideConfigurerWithPropertiesFile() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
		poc.setLocation(TEST_PROPS);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithInvalidPropertiesFile() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
		poc.setLocations(TEST_PROPS, XTEST_PROPS);
		poc.setIgnoreResourceNotFound(true);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithPropertiesXmlFile() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
		poc.setLocation(TEST_PROPS_XML);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertEquals(99, tb.getArray()[0].getAge());
		assertEquals("test", ((TestBean) tb.getList().get(1)).getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithConvertProperties() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		ConvertingOverrideConfigurer bfpp = new ConvertingOverrideConfigurer();
		Properties props = new Properties();
		props.setProperty("tb.array[0].name", "99");
		props.setProperty("tb.list[1].name", "test");
		bfpp.setProperties(props);
		bfpp.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertEquals("X99", tb.getArray()[0].getName());
		assertEquals("Xtest", ((TestBean) tb.getList().get(1)).getName());
	}

	@Test
	public void testPropertyOverrideConfigurerWithInvalidKey() {
		factory.registerBeanDefinition("tb1", genericBeanDefinition(TestBean.class).getBeanDefinition());
		factory.registerBeanDefinition("tb2", genericBeanDefinition(TestBean.class).getBeanDefinition());

		{
			PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
			poc.setIgnoreInvalidKeys(true);
			Properties props = new Properties();
			props.setProperty("argh", "hgra");
			props.setProperty("tb2.name", "test");
			props.setProperty("tb2.nam", "test");
			props.setProperty("tb3.name", "test");
			poc.setProperties(props);
			poc.postProcessBeanFactory(factory);
			assertEquals("test", factory.getBean("tb2", TestBean.class).getName());
		}
		{
			PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
			Properties props = new Properties();
			props.setProperty("argh", "hgra");
			props.setProperty("tb2.age", "99");
			props.setProperty("tb2.name", "test2");
			poc.setProperties(props);
			poc.setOrder(0); // won't actually do anything since we're not processing through an app ctx
			try {
				poc.postProcessBeanFactory(factory);
			}
			catch (BeanInitializationException ex) {
				// prove that the processor chokes on the invalid key
				assertTrue(ex.getMessage().toLowerCase().contains("argh"));
			}
		}
	}

	@Test
	public void testPropertyOverrideConfigurerWithIgnoreInvalidKeys() {
		factory.registerBeanDefinition("tb1", genericBeanDefinition(TestBean.class).getBeanDefinition());
		factory.registerBeanDefinition("tb2", genericBeanDefinition(TestBean.class).getBeanDefinition());

		{
			PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
			Properties props = new Properties();
			props.setProperty("tb2.age", "99");
			props.setProperty("tb2.name", "test2");
			poc.setProperties(props);
			poc.setOrder(0); // won't actually do anything since we're not processing through an app ctx
			poc.postProcessBeanFactory(factory);
		}
		{
			PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
			poc.setIgnoreInvalidKeys(true);
			Properties props = new Properties();
			props.setProperty("argh", "hgra");
			props.setProperty("tb1.age", "99");
			props.setProperty("tb2.name", "test");
			poc.setProperties(props);
			poc.postProcessBeanFactory(factory);
		}

		TestBean tb1 = (TestBean) factory.getBean("tb1");
		TestBean tb2 = (TestBean) factory.getBean("tb2");
		assertEquals(99, tb1.getAge());
		assertEquals(99, tb2.getAge());
		assertEquals(null, tb1.getName());
		assertEquals("test", tb2.getName());
	}

	@Test
	public void testPropertyPlaceholderConfigurer() {
		doTestPropertyPlaceholderConfigurer(false);
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithParentChildSeparation() {
		doTestPropertyPlaceholderConfigurer(true);
	}

	private void doTestPropertyPlaceholderConfigurer(boolean parentChildSeparation) {
		Map singletonMap = Collections.singletonMap("myKey", "myValue");
		if (parentChildSeparation) {
			MutablePropertyValues pvs1 = new MutablePropertyValues();
			pvs1.add("age", "${age}");
			MutablePropertyValues pvs2 = new MutablePropertyValues();
			pvs2.add("name", "name${var}${var}${");
			pvs2.add("spouse", new RuntimeBeanReference("${ref}"));
			pvs2.add("someMap", singletonMap);
			RootBeanDefinition parent = new RootBeanDefinition(TestBean.class);
			parent.setPropertyValues(pvs1);
			ChildBeanDefinition bd = new ChildBeanDefinition("${parent}", pvs2);
			factory.registerBeanDefinition("parent1", parent);
			factory.registerBeanDefinition("tb1", bd);
		}
		else {
			MutablePropertyValues pvs = new MutablePropertyValues();
			pvs.add("age", "${age}");
			pvs.add("name", "name${var}${var}${");
			pvs.add("spouse", new RuntimeBeanReference("${ref}"));
			pvs.add("someMap", singletonMap);
			RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
			bd.setPropertyValues(pvs);
			factory.registerBeanDefinition("tb1", bd);
		}

		ConstructorArgumentValues cas = new ConstructorArgumentValues();
		cas.addIndexedArgumentValue(1, "${age}");
		cas.addGenericArgumentValue("${var}name${age}");

		MutablePropertyValues pvs = new MutablePropertyValues();
		pvs.add("stringArray", new String[] {"${os.name}", "${age}"});

		List<Object> friends = new ManagedList<>();
		friends.add("na${age}me");
		friends.add(new RuntimeBeanReference("${ref}"));
		pvs.add("friends", friends);

		Set<Object> someSet = new ManagedSet<>();
		someSet.add("na${age}me");
		someSet.add(new RuntimeBeanReference("${ref}"));
		someSet.add(new TypedStringValue("${age}", Integer.class));
		pvs.add("someSet", someSet);

		Map<Object, Object> someMap = new ManagedMap<>();
		someMap.put(new TypedStringValue("key${age}"), new TypedStringValue("${age}"));
		someMap.put(new TypedStringValue("key${age}ref"), new RuntimeBeanReference("${ref}"));
		someMap.put("key1", new RuntimeBeanReference("${ref}"));
		someMap.put("key2", "${age}name");
		MutablePropertyValues innerPvs = new MutablePropertyValues();
		innerPvs.add("country", "${os.name}");
		RootBeanDefinition innerBd = new RootBeanDefinition(TestBean.class);
		innerBd.setPropertyValues(innerPvs);
		someMap.put("key3", innerBd);
		MutablePropertyValues innerPvs2 = new MutablePropertyValues(innerPvs);
		someMap.put("${key4}", new BeanDefinitionHolder(new ChildBeanDefinition("tb1", innerPvs2), "child"));
		pvs.add("someMap", someMap);

		RootBeanDefinition bd = new RootBeanDefinition(TestBean.class, cas, pvs);
		factory.registerBeanDefinition("tb2", bd);

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("age", "98");
		props.setProperty("var", "${m}var");
		props.setProperty("ref", "tb2");
		props.setProperty("m", "my");
		props.setProperty("key4", "mykey4");
		props.setProperty("parent", "parent1");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb1 = (TestBean) factory.getBean("tb1");
		TestBean tb2 = (TestBean) factory.getBean("tb2");
		assertEquals(98, tb1.getAge());
		assertEquals(98, tb2.getAge());
		assertEquals("namemyvarmyvar${", tb1.getName());
		assertEquals("myvarname98", tb2.getName());
		assertEquals(tb2, tb1.getSpouse());
		assertEquals(1, tb1.getSomeMap().size());
		assertEquals("myValue", tb1.getSomeMap().get("myKey"));
		assertEquals(2, tb2.getStringArray().length);
		assertEquals(System.getProperty("os.name"), tb2.getStringArray()[0]);
		assertEquals("98", tb2.getStringArray()[1]);
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
		assertEquals(System.getProperty("os.name"), inner1.getCountry());
		assertEquals(98, inner2.getAge());
		assertEquals("namemyvarmyvar${", inner2.getName());
		assertEquals(System.getProperty("os.name"), inner2.getCountry());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithSystemPropertyFallback() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("country", "${os.name}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals(System.getProperty("os.name"), tb.getCountry());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithSystemPropertyNotUsed() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("country", "${os.name}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("os.name", "myos");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("myos", tb.getCountry());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithOverridingSystemProperty() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("country", "${os.name}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("os.name", "myos");
		ppc.setProperties(props);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals(System.getProperty("os.name"), tb.getCountry());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithUnresolvableSystemProperty() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("touchy", "${user.dir}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_NEVER);

		try {
			ppc.postProcessBeanFactory(factory);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getMessage().contains("user.dir"));
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithUnresolvablePlaceholder() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${ref}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();

		try {
			ppc.postProcessBeanFactory(factory);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
			assertTrue(ex.getMessage().contains("ref"));
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithIgnoreUnresolvablePlaceholder() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${ref}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("${ref}", tb.getName());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithEmptyStringAsNull() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setNullValue("");
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertNull(tb.getName());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithEmptyStringInPlaceholderAsNull() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${ref}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setNullValue("");
		Properties props = new Properties();
		props.put("ref", "");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertNull(tb.getName());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithNestedPlaceholderInKey() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my${key}key}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("key", "new");
		props.put("mynewkey", "myname");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("myname", tb.getName());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithPlaceholderInAlias() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class).getBeanDefinition());
		factory.registerAlias("tb", "${alias}");

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("alias", "tb2");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		TestBean tb2 = (TestBean) factory.getBean("tb2");
		assertSame(tb, tb2);
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithSelfReferencingPlaceholderInAlias() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class).getBeanDefinition());
		factory.registerAlias("tb", "${alias}");

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("alias", "tb");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertNotNull(tb);
		assertEquals(0, factory.getAliases("tb").length);
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithCircularReference() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("age", "${age}")
				.addPropertyValue("name", "name${var}")
				.getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("age", "99");
		props.setProperty("var", "${m}");
		props.setProperty("m", "${var}");
		ppc.setProperties(props);

		try {
			ppc.postProcessBeanFactory(factory);
			fail("Should have thrown BeanDefinitionStoreException");
		}
		catch (BeanDefinitionStoreException ex) {
			// expected
		}
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithDefaultProperties() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("touchy", "${test}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("test", "mytest");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithInlineDefault() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("touchy", "${test:mytest}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithAliases() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("touchy", "${test}").getBeanDefinition());

		factory.registerAlias("tb", "${myAlias}");
		factory.registerAlias("${myTarget}", "alias2");

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("test", "mytest");
		props.put("myAlias", "alias");
		props.put("myTarget", "tb");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("mytest", tb.getTouchy());
		tb = (TestBean) factory.getBean("alias");
		assertEquals("mytest", tb.getTouchy());
		tb = (TestBean) factory.getBean("alias2");
		assertEquals("mytest", tb.getTouchy());
	}

	@Test
	public void testPreferencesPlaceholderConfigurer() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${myName}")
				.addPropertyValue("age", "${myAge}")
				.addPropertyValue("touchy", "${myTouchy}")
				.getBeanDefinition());

		PreferencesPlaceholderConfigurer ppc = new PreferencesPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("myAge", "99");
		ppc.setProperties(props);
		Preferences.systemRoot().put("myName", "myNameValue");
		Preferences.systemRoot().put("myTouchy", "myTouchyValue");
		Preferences.userRoot().put("myTouchy", "myOtherTouchyValue");
		ppc.afterPropertiesSet();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("myNameValue", tb.getName());
		assertEquals(99, tb.getAge());
		assertEquals("myOtherTouchyValue", tb.getTouchy());
		Preferences.userRoot().remove("myTouchy");
		Preferences.systemRoot().remove("myTouchy");
		Preferences.systemRoot().remove("myName");
	}

	@Test
	public void testPreferencesPlaceholderConfigurerWithCustomTreePaths() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${myName}")
				.addPropertyValue("age", "${myAge}")
				.addPropertyValue("touchy", "${myTouchy}")
				.getBeanDefinition());

		PreferencesPlaceholderConfigurer ppc = new PreferencesPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("myAge", "99");
		ppc.setProperties(props);
		ppc.setSystemTreePath("mySystemPath");
		ppc.setUserTreePath("myUserPath");
		Preferences.systemRoot().node("mySystemPath").put("myName", "myNameValue");
		Preferences.systemRoot().node("mySystemPath").put("myTouchy", "myTouchyValue");
		Preferences.userRoot().node("myUserPath").put("myTouchy", "myOtherTouchyValue");
		ppc.afterPropertiesSet();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("myNameValue", tb.getName());
		assertEquals(99, tb.getAge());
		assertEquals("myOtherTouchyValue", tb.getTouchy());
		Preferences.userRoot().node("myUserPath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath").remove("myName");
	}

	@Test
	public void testPreferencesPlaceholderConfigurerWithPathInPlaceholder() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${mypath/myName}")
				.addPropertyValue("age", "${myAge}")
				.addPropertyValue("touchy", "${myotherpath/myTouchy}")
				.getBeanDefinition());

		PreferencesPlaceholderConfigurer ppc = new PreferencesPlaceholderConfigurer();
		Properties props = new Properties();
		props.put("myAge", "99");
		ppc.setProperties(props);
		ppc.setSystemTreePath("mySystemPath");
		ppc.setUserTreePath("myUserPath");
		Preferences.systemRoot().node("mySystemPath").node("mypath").put("myName", "myNameValue");
		Preferences.systemRoot().node("mySystemPath/myotherpath").put("myTouchy", "myTouchyValue");
		Preferences.userRoot().node("myUserPath/myotherpath").put("myTouchy", "myOtherTouchyValue");
		ppc.afterPropertiesSet();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertEquals("myNameValue", tb.getName());
		assertEquals(99, tb.getAge());
		assertEquals("myOtherTouchyValue", tb.getTouchy());
		Preferences.userRoot().node("myUserPath/myotherpath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath/myotherpath").remove("myTouchy");
		Preferences.systemRoot().node("mySystemPath/mypath").remove("myName");
	}


	static class PropertiesHolder {

		private Properties props = new Properties();

		public Properties getHeldProperties() {
			return props;
		}

		public void setHeldProperties(Properties props) {
			this.props = props;
		}
	}


	private static class ConvertingOverrideConfigurer extends PropertyOverrideConfigurer {

		@Override
		protected String convertPropertyValue(String originalValue) {
			return "X" + originalValue;
		}
	}


	/**
	 * {@link PreferencesFactory} to create {@link MockPreferences}.
	 */
	public static class MockPreferencesFactory implements PreferencesFactory {

		private final Preferences userRoot = new MockPreferences();

		private final Preferences systemRoot = new MockPreferences();

		@Override
		public Preferences systemRoot() {
			return this.systemRoot;
		}

		@Override
		public Preferences userRoot() {
			return this.userRoot;
		}
	}


	/**
	 * Mock implementation of {@link Preferences} that behaves the same regardless of the
	 * underlying operating system and will never throw security exceptions.
	 */
	public static class MockPreferences extends AbstractPreferences {

		private static Map<String, String> values = new HashMap<>();

		private static Map<String, AbstractPreferences> children = new HashMap<>();

		public MockPreferences() {
			super(null, "");
		}

		protected MockPreferences(AbstractPreferences parent, String name) {
			super(parent, name);
		}

		@Override
		protected void putSpi(String key, String value) {
			values.put(key, value);
		}

		@Override
		protected String getSpi(String key) {
			return values.get(key);
		}

		@Override
		protected void removeSpi(String key) {
			values.remove(key);
		}

		@Override
		protected void removeNodeSpi() throws BackingStoreException {
		}

		@Override
		protected String[] keysSpi() throws BackingStoreException {
			return StringUtils.toStringArray(values.keySet());
		}

		@Override
		protected String[] childrenNamesSpi() throws BackingStoreException {
			return StringUtils.toStringArray(children.keySet());
		}

		@Override
		protected AbstractPreferences childSpi(String name) {
			AbstractPreferences child = children.get(name);
			if (child == null) {
				child = new MockPreferences(this, name);
				children.put(name, child);
			}
			return child;
		}

		@Override
		protected void syncSpi() throws BackingStoreException {
		}

		@Override
		protected void flushSpi() throws BackingStoreException {
		}
	}

}
