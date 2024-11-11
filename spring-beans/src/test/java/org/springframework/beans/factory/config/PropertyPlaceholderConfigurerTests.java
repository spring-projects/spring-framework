/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.registerWithGeneratedName;

/**
 * Tests for {@link PropertyPlaceholderConfigurer}.
 *
 * @author Chris Beams
 * @author Sam Brannen
 */
@SuppressWarnings("deprecation")
class PropertyPlaceholderConfigurerTests {

	private static final String P1 = "p1";
	private static final String P1_LOCAL_PROPS_VAL = "p1LocalPropsVal";
	private static final String P1_SYSTEM_PROPS_VAL = "p1SystemPropsVal";

	private final DefaultListableBeanFactory bf = new DefaultListableBeanFactory();

	private final PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();

	private final Properties ppcProperties = new Properties();

	private AbstractBeanDefinition p1BeanDef = rootBeanDefinition(TestBean.class)
			.addPropertyValue("name", "${" + P1 + "}")
			.getBeanDefinition();


	@BeforeEach
	void setup() {
		ppcProperties.setProperty(P1, P1_LOCAL_PROPS_VAL);
		System.setProperty(P1, P1_SYSTEM_PROPS_VAL);
		ppc.setProperties(ppcProperties);
	}

	@AfterEach
	void cleanup() {
		System.clearProperty(P1);
		System.clearProperty(P1_SYSTEM_PROPS_VAL);
	}


	@Test
	void localPropertiesViaResource() {
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
	void resolveFromSystemProperties() {
		System.setProperty("otherKey", "systemValue");
		p1BeanDef = rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${" + P1 + "}")
				.addPropertyValue("sex", "${otherKey}")
				.getBeanDefinition();
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
		assertThat(bean.getSex()).isEqualTo("systemValue");
		System.clearProperty("otherKey");
	}

	@Test
	void resolveFromLocalProperties() {
		System.clearProperty(P1);
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
	}

	@Test
	void setSystemPropertiesMode_defaultIsFallback() {
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
	}

	@Test
	void setSystemSystemPropertiesMode_toOverride_andResolveFromSystemProperties() {
		registerWithGeneratedName(p1BeanDef, bf);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName()).isEqualTo(P1_SYSTEM_PROPS_VAL);
	}

	@Test
	void setSystemSystemPropertiesMode_toOverride_andSetSearchSystemEnvironment_toFalse() {
		registerWithGeneratedName(p1BeanDef, bf);
		System.clearProperty(P1); // will now fall all the way back to system environment
		ppc.setSearchSystemEnvironment(false);
		ppc.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
		ppc.postProcessBeanFactory(bf);
		TestBean bean = bf.getBean(TestBean.class);
		assertThat(bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL); // has to resort to local props
	}

	/**
	 * Creates a scenario in which two PPCs are configured, each with different
	 * settings regarding resolving properties from the environment.
	 */
	@Test
	void twoPlaceholderConfigurers_withConflictingSettings() {
		String P2 = "p2";
		String P2_LOCAL_PROPS_VAL = "p2LocalPropsVal";
		String P2_SYSTEM_PROPS_VAL = "p2SystemPropsVal";

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
		assertThat(p1Bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);

		TestBean p2Bean = bf.getBean("p2Bean", TestBean.class);
		assertThat(p2Bean.getName()).isEqualTo(P1_LOCAL_PROPS_VAL);
		assertThat(p2Bean.getCountry()).isEqualTo(P2_SYSTEM_PROPS_VAL);

		System.clearProperty(P2);
	}

	@Test
	void customPlaceholderPrefixAndSuffix() {
		ppc.setPlaceholderPrefix("@<");
		ppc.setPlaceholderSuffix(">");

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

		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("systemKey1Value");
		assertThat(bf.getBean(TestBean.class).getSex()).isEqualTo("${key2}");
	}

	@Test
	void nullValueIsPreserved() {
		ppc.setNullValue("customNull");
		System.setProperty("my.name", "customNull");
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isNull();
		System.clearProperty("my.name");
	}

	@Test
	void trimValuesIsOffByDefault() {
		System.setProperty("my.name", " myValue  ");
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo(" myValue  ");
		System.clearProperty("my.name");
	}

	@Test
	void trimValuesIsApplied() {
		ppc.setTrimValues(true);
		System.setProperty("my.name", " myValue  ");
		bf.registerBeanDefinition("testBean", rootBeanDefinition(TestBean.class)
				.addPropertyValue("name", "${my.name}")
				.getBeanDefinition());
		ppc.postProcessBeanFactory(bf);
		assertThat(bf.getBean(TestBean.class).getName()).isEqualTo("myValue");
		System.clearProperty("my.name");
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all SYSTEM_PROPERTIES_MODE_ constants defined in
	 * {@link PropertyPlaceholderConfigurer}.
	 */
	@Test
	@SuppressWarnings("deprecation")
	void setSystemPropertiesModeNameToAllSupportedValues() {
		streamSystemPropertiesModeConstants()
				.map(Field::getName)
				.forEach(name -> assertThatNoException().as(name).isThrownBy(() -> ppc.setSystemPropertiesModeName(name)));
	}

	private static Stream<Field> streamSystemPropertiesModeConstants() {
		return Arrays.stream(PropertyPlaceholderConfigurer.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.filter(field -> field.getName().startsWith("SYSTEM_PROPERTIES_MODE_"));
	}

}
