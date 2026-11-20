/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Tests for {@link PropertiesFactoryBean}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 01.11.2003
 */
class PropertiesFactoryBeanTests {

	private static final Class<?> CLASS = PropertiesFactoryBeanTests.class;
	private static final Resource TEST_PROPS = qualifiedResource(CLASS, "test.properties");
	private static final Resource TEST_PROPS_XML = qualifiedResource(CLASS, "test.properties.xml");

	@Test
	void withPropertiesFile() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(TEST_PROPS);
		pfb.afterPropertiesSet();
		Properties props = pfb.getObject();
		assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
	}

	@Test
	void withPropertiesXmlFile() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(TEST_PROPS_XML);
		pfb.afterPropertiesSet();
		Properties props = pfb.getObject();
		assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
	}

	@Test
	void withLocalProperties() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		pfb.setProperties(localProps);
		pfb.afterPropertiesSet();
		Properties props = pfb.getObject();
		assertThat(props.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void withPropertiesFileAndLocalProperties() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(TEST_PROPS);
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		localProps.setProperty("tb.array[0].age", "0");
		pfb.setProperties(localProps);
		pfb.afterPropertiesSet();
		Properties props = pfb.getObject();
		assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
		assertThat(props.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void withPropertiesFileAndMultipleLocalProperties() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(TEST_PROPS);

		Properties props1 = new Properties();
		props1.setProperty("key2", "value2");
		props1.setProperty("tb.array[0].age", "0");

		Properties props2 = new Properties();
		props2.setProperty("spring", "framework");
		props2.setProperty("Don", "Mattingly");

		Properties props3 = new Properties();
		props3.setProperty("spider", "man");
		props3.setProperty("bat", "man");

		pfb.setPropertiesArray(new Properties[] {props1, props2, props3});
		pfb.afterPropertiesSet();

		Properties props = pfb.getObject();
		assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
		assertThat(props.getProperty("key2")).isEqualTo("value2");
		assertThat(props.getProperty("spring")).isEqualTo("framework");
		assertThat(props.getProperty("Don")).isEqualTo("Mattingly");
		assertThat(props.getProperty("spider")).isEqualTo("man");
		assertThat(props.getProperty("bat")).isEqualTo("man");
	}

	@Test
	void withPropertiesFileAndLocalPropertiesAndLocalOverride() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(TEST_PROPS);
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		localProps.setProperty("tb.array[0].age", "0");
		pfb.setProperties(localProps);
		pfb.setLocalOverride(true);
		pfb.afterPropertiesSet();
		Properties props = pfb.getObject();
		assertThat(props.getProperty("tb.array[0].age")).isEqualTo("0");
		assertThat(props.getProperty("key2")).isEqualTo("value2");
	}

	@Test
	void withPrototype() throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setSingleton(false);
		pfb.setLocation(TEST_PROPS);
		Properties localProps = new Properties();
		localProps.setProperty("key2", "value2");
		pfb.setProperties(localProps);
		pfb.afterPropertiesSet();
		Properties props = pfb.getObject();
		assertThat(props.getProperty("tb.array[0].age")).isEqualTo("99");
		assertThat(props.getProperty("key2")).isEqualTo("value2");
		Properties newProps = pfb.getObject();
		assertThat(props).isNotSameAs(newProps);
		assertThat(newProps.getProperty("tb.array[0].age")).isEqualTo("99");
		assertThat(newProps.getProperty("key2")).isEqualTo("value2");
	}

}
