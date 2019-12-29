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

import org.junit.jupiter.api.Test;

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
import org.springframework.beans.testfixture.beans.IndexedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

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
@SuppressWarnings("deprecation")
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

		assertThat(tb1.getAge()).isEqualTo(99);
		assertThat(tb2.getAge()).isEqualTo(99);
		assertThat(tb1.getName()).isEqualTo(null);
		assertThat(tb2.getName()).isEqualTo("test");
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
		assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
		assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
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
		assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
		assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
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
		assertThat(tb.getMap().get("key1")).isEqualTo("99");
		assertThat(tb.getMap().get("key2.ext")).isEqualTo("test");
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
		assertThat(tb.getHeldProperties().getProperty("mail.smtp.auth")).isEqualTo("true");
	}

	@Test
	public void testPropertyOverrideConfigurerWithPropertiesFile() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
		poc.setLocation(TEST_PROPS);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
		assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
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
		assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
		assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
	}

	@Test
	public void testPropertyOverrideConfigurerWithPropertiesXmlFile() {
		BeanDefinition def = BeanDefinitionBuilder.genericBeanDefinition(IndexedTestBean.class).getBeanDefinition();
		factory.registerBeanDefinition("tb", def);

		PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
		poc.setLocation(TEST_PROPS_XML);
		poc.postProcessBeanFactory(factory);

		IndexedTestBean tb = (IndexedTestBean) factory.getBean("tb");
		assertThat(tb.getArray()[0].getAge()).isEqualTo(99);
		assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("test");
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
		assertThat(tb.getArray()[0].getName()).isEqualTo("X99");
		assertThat(((TestBean) tb.getList().get(1)).getName()).isEqualTo("Xtest");
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
			assertThat(factory.getBean("tb2", TestBean.class).getName()).isEqualTo("test");
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
				assertThat(ex.getMessage().toLowerCase().contains("argh")).isTrue();
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
		assertThat(tb1.getAge()).isEqualTo(99);
		assertThat(tb2.getAge()).isEqualTo(99);
		assertThat(tb1.getName()).isEqualTo(null);
		assertThat(tb2.getName()).isEqualTo("test");
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
		Map<String, String> singletonMap = Collections.singletonMap("myKey", "myValue");
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
		assertThat(tb1.getAge()).isEqualTo(98);
		assertThat(tb2.getAge()).isEqualTo(98);
		assertThat(tb1.getName()).isEqualTo("namemyvarmyvar${");
		assertThat(tb2.getName()).isEqualTo("myvarname98");
		assertThat(tb1.getSpouse()).isEqualTo(tb2);
		assertThat(tb1.getSomeMap().size()).isEqualTo(1);
		assertThat(tb1.getSomeMap().get("myKey")).isEqualTo("myValue");
		assertThat(tb2.getStringArray().length).isEqualTo(2);
		assertThat(tb2.getStringArray()[0]).isEqualTo(System.getProperty("os.name"));
		assertThat(tb2.getStringArray()[1]).isEqualTo("98");
		assertThat(tb2.getFriends().size()).isEqualTo(2);
		assertThat(tb2.getFriends().iterator().next()).isEqualTo("na98me");
		assertThat(tb2.getFriends().toArray()[1]).isEqualTo(tb2);
		assertThat(tb2.getSomeSet().size()).isEqualTo(3);
		assertThat(tb2.getSomeSet().contains("na98me")).isTrue();
		assertThat(tb2.getSomeSet().contains(tb2)).isTrue();
		assertThat(tb2.getSomeSet().contains(new Integer(98))).isTrue();
		assertThat(tb2.getSomeMap().size()).isEqualTo(6);
		assertThat(tb2.getSomeMap().get("key98")).isEqualTo("98");
		assertThat(tb2.getSomeMap().get("key98ref")).isEqualTo(tb2);
		assertThat(tb2.getSomeMap().get("key1")).isEqualTo(tb2);
		assertThat(tb2.getSomeMap().get("key2")).isEqualTo("98name");
		TestBean inner1 = (TestBean) tb2.getSomeMap().get("key3");
		TestBean inner2 = (TestBean) tb2.getSomeMap().get("mykey4");
		assertThat(inner1.getAge()).isEqualTo(0);
		assertThat(inner1.getName()).isEqualTo(null);
		assertThat(inner1.getCountry()).isEqualTo(System.getProperty("os.name"));
		assertThat(inner2.getAge()).isEqualTo(98);
		assertThat(inner2.getName()).isEqualTo("namemyvarmyvar${");
		assertThat(inner2.getCountry()).isEqualTo(System.getProperty("os.name"));
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithSystemPropertyFallback() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("country", "${os.name}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertThat(tb.getCountry()).isEqualTo(System.getProperty("os.name"));
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
		assertThat(tb.getCountry()).isEqualTo("myos");
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
		assertThat(tb.getCountry()).isEqualTo(System.getProperty("os.name"));
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithUnresolvableSystemProperty() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("touchy", "${user.dir}").getBeanDefinition());
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_NEVER);
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				ppc.postProcessBeanFactory(factory))
			.withMessageContaining("user.dir");
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithUnresolvablePlaceholder() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${ref}").getBeanDefinition());
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				ppc.postProcessBeanFactory(factory))
			.withMessageContaining("ref");
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithIgnoreUnresolvablePlaceholder() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${ref}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertThat(tb.getName()).isEqualTo("${ref}");
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithEmptyStringAsNull() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("name", "").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setNullValue("");
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertThat(tb.getName()).isNull();
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
		assertThat(tb.getName()).isNull();
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
		assertThat(tb.getName()).isEqualTo("myname");
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
		assertThat(tb2).isSameAs(tb);
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
		assertThat(tb).isNotNull();
		assertThat(factory.getAliases("tb").length).isEqualTo(0);
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
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				ppc.postProcessBeanFactory(factory));
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
		assertThat(tb.getTouchy()).isEqualTo("mytest");
	}

	@Test
	public void testPropertyPlaceholderConfigurerWithInlineDefault() {
		factory.registerBeanDefinition("tb", genericBeanDefinition(TestBean.class)
				.addPropertyValue("touchy", "${test:mytest}").getBeanDefinition());

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.postProcessBeanFactory(factory);

		TestBean tb = (TestBean) factory.getBean("tb");
		assertThat(tb.getTouchy()).isEqualTo("mytest");
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
		assertThat(tb.getTouchy()).isEqualTo("mytest");
		tb = (TestBean) factory.getBean("alias");
		assertThat(tb.getTouchy()).isEqualTo("mytest");
		tb = (TestBean) factory.getBean("alias2");
		assertThat(tb.getTouchy()).isEqualTo("mytest");
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
		assertThat(tb.getName()).isEqualTo("myNameValue");
		assertThat(tb.getAge()).isEqualTo(99);
		assertThat(tb.getTouchy()).isEqualTo("myOtherTouchyValue");
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
		assertThat(tb.getName()).isEqualTo("myNameValue");
		assertThat(tb.getAge()).isEqualTo(99);
		assertThat(tb.getTouchy()).isEqualTo("myOtherTouchyValue");
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
		assertThat(tb.getName()).isEqualTo("myNameValue");
		assertThat(tb.getAge()).isEqualTo(99);
		assertThat(tb.getTouchy()).isEqualTo("myOtherTouchyValue");
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
