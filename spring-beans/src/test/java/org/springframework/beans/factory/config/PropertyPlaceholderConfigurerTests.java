/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.tests.sample.beans.TestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;
import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.*;

/**
 * Unit tests for {@link PropertyPlaceholderConfigurer}.
 *
 * @author Chris Beams
 */
public class PropertyPlaceholderConfigurerTests {

	private static final String P1 = "p1";
	private static final String P1_LOCAL_PROPS_VAL = "p1LocalPropsVal";
	private static final String P1_SYSTEM_PROPS_VAL = "p1SystemPropsVal";
	private static final String P1_SYSTEM_ENV_VAL = "p1SystemEnvVal";

	private DefaultListableBeanFactory bf;
	private PropertyPlaceholderConfigurer ppc;
	private Properties ppcProperties;

	private AbstractBeanDefinition p1BeanDef;


	@Before
	public void setup() {
		p1BeanDef = rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${" + P1 + "}")
				.getBeanDefinition();

		bf = new DefaultListableBeanFactory();

		ppcProperties = new Properties();
		ppcProperties.setProperty(P1, P1_LOCAL_PROPS_VAL);
		System.setProperty(P1, P1_SYSTEM_PROPS_VAL);
		ppc = new PropertyPlaceholderConfigurer();
		ppc.setProperties(ppcProperties);

	}

	@After
	public void cleanup() {
		System.clearProperty(P1);
	}


	@Test
	public void localPropertiesViaResource() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				genericBeanDefinition(TestBean.class)
					.addPropertyValue("name", "${my.name}")
					.getBeanDefinition());

		PropertyPlaceholderConfigurer pc = new PropertyPlaceholderConfigurer();
		Resource resource = new ClassPathResource("PropertyPlaceholderConfigurerTests.properties", this.getClass());
		pc.setLocation(resource);
		pc.postProcessBeanFactory(bf);
	}

	@Test
	public void resolveFromSystemProperties() {
		System.setProperty("otherKey", "systemValue");
		p1BeanDef = rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${" + P1 + "}")
				.addPropertyValue("sex", "${otherKey}")
				.getBeanDefinition();
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
		assertThat(bean.getSex(), equalTo("systemValue"));
		System.clearProperty("otherKey");
	}

	@Test
	public void resolveFromLocalProperties() {
		System.clearProperty(P1);
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
	}

	@Test
	public void setSystemPropertiesMode_defaultIsFallback() {
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
	}

	@Test
	public void setSystemSystemPropertiesMode_toOverride_andResolveFromSystemProperties() {
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_SYSTEM_PROPS_VAL));
	}

	@Test
	public void setSystemSystemPropertiesMode_toOverride_andSetSearchSystemEnvironment_toFalse() {
		registerWithGeneratedName(p1BeanDef, bf);
		System.clearProperty(P1); // will now fall all the way back to system environment
		ppc.setSearchSystemEnvironment(false);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName(), equalTo(P1_LOCAL_PROPS_VAL)); // has to resort to local props
	}

	/**
	 * Creates a scenario in which two PPCs are configured, each with different
	 * settings regarding resolving properties from the environment.
	 */
	@Test
	public void twoPlaceholderConfigurers_withConflictingSettings() {
		String P2 = "p2";
		String P2_LOCAL_PROPS_VAL = "p2LocalPropsVal";
		String P2_SYSTEM_PROPS_VAL = "p2SystemPropsVal";
		String P2_SYSTEM_ENV_VAL = "p2SystemEnvVal";

		AbstractBeanDefinition p2BeanDef = rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${" + P1 + "}")
				.addPropertyValue("country", "${" + P2 + "}")
				.getBeanDefinition();

		bf.registerBeanDefinition("p1Bean", p1BeanDef);
		bf.registerBeanDefinition("p2Bean", p2BeanDef);

		ppc.setIgnoreUnresolvablePlaceholders(true);
		ppc.postProcessBeanFactory(bf);

		System.setProperty(P2, P2_SYSTEM_PROPS_VAL);
		Properties ppc2Properties = new Properties();
		ppc2Properties.put(P2, P2_LOCAL_PROPS_VAL);

		PropertyPlaceholderConfigurer ppc2 = new PropertyPlaceholderConfigurer();
		ppc2.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc2.setProperties(ppc2Properties);

		ppc2Properties = new Properties();
		ppc2Properties.setProperty(P2, P2_LOCAL_PROPS_VAL);
		ppc2.postProcessBeanFactory(bf);

		TestBean p1Bean = bf.getBean("p1Bean", TestBean.class);
		assertThat(p1Bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));

		TestBean p2Bean = bf.getBean("p2Bean", TestBean.class);
		assertThat(p2Bean.getName(), equalTo(P1_LOCAL_PROPS_VAL));
		assertThat(p2Bean.getCountry(), equalTo(P2_SYSTEM_PROPS_VAL));

		System.clearProperty(P2);
	}

	@Test
	public void customPlaceholderPrefixAndSuffix() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setPlaceholderPrefix("@<");
		ppc.setPlaceholderSuffix(">");

		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean",
				rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "@<key1>")
				.addPropertyValue("sex", "${key2}")
				.getBeanDefinition());

		System.setProperty("key1", "systemKey1Value");
		System.setProperty("key2", "systemKey2Value");
		ppc.postProcessBeanFactory(bf);
		System.clearProperty("key1");
		System.clearProperty("key2");

		assertThat(bf.getBean(TestBean.class).getName(), is("systemKey1Value"));
		assertThat(bf.getBean(TestBean.class).getSex(), is("${key2}"));
	}

	@Test
	public void nullValueIsPreserved() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setNullValue("customNull");
		System.setProperty("my.name", "customNull");
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), nullValue());
		System.clearProperty("my.name");
	}

	@Test
	public void trimValuesIsOffByDefault() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		System.setProperty("my.name", " myValue  ");
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo(" myValue  "));
		System.clearProperty("my.name");
	}

	@Test
	public void trimValuesIsApplied() {
		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		ppc.setTrimValues(true);
		System.setProperty("my.name", " myValue  ");
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName(), equalTo("myValue"));
		System.clearProperty("my.name");
	}

}
