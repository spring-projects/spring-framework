/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.naming;

import java.util.function.Consumer;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


/**
 * Tests for {@link MetadataNamingStrategy}.
 *
 * @author Stephane Nicoll
 */
class MetadataNamingStrategyTests {

	private static final TestBean TEST_BEAN = new TestBean();

	private final MetadataNamingStrategy strategy;

	MetadataNamingStrategyTests() {
		this.strategy = new MetadataNamingStrategy();
		this.strategy.setDefaultDomain("com.example");
		this.strategy.setAttributeSource(new AnnotationJmxAttributeSource());
	}

	@Test
	void getObjectNameWhenBeanNameIsSimple() throws MalformedObjectNameException {
		ObjectName name = this.strategy.getObjectName(TEST_BEAN, "myBean");
		assertThat(name.getDomain()).isEqualTo("com.example");
		assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "myBean"));
	}

	@Test
	void getObjectNameWhenBeanNameIsValidObjectName() throws MalformedObjectNameException {
		ObjectName name = this.strategy.getObjectName(TEST_BEAN, "com.another:name=myBean");
		assertThat(name.getDomain()).isEqualTo("com.another");
		assertThat(name.getKeyPropertyList()).containsOnly(entry("name", "myBean"));
	}

	@Test
	void getObjectNameWhenBeanNameContainsComma() throws MalformedObjectNameException {
		ObjectName name = this.strategy.getObjectName(TEST_BEAN, "myBean,");
		assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"myBean,\""));
	}

	@Test
	void getObjectNameWhenBeanNameContainsEquals() throws MalformedObjectNameException {
		ObjectName name = this.strategy.getObjectName(TEST_BEAN, "my=Bean");
		assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"my=Bean\""));
	}

	@Test
	void getObjectNameWhenBeanNameContainsColon() throws MalformedObjectNameException {
		ObjectName name = this.strategy.getObjectName(TEST_BEAN, "my:Bean");
		assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"my:Bean\""));
	}

	@Test
	void getObjectNameWhenBeanNameContainsQuote() throws MalformedObjectNameException {
		ObjectName name = this.strategy.getObjectName(TEST_BEAN, "\"myBean\"");
		assertThat(name).satisfies(hasDefaultProperties(TEST_BEAN, "\"\\\"myBean\\\"\""));
	}

	private Consumer<ObjectName> hasDefaultProperties(Object instance, String expectedName) {
		return objectName -> assertThat(objectName.getKeyPropertyList()).containsOnly(
				entry("type", ClassUtils.getShortName(instance.getClass())),
				entry("name", expectedName));
	}

	static class TestBean {}

}
