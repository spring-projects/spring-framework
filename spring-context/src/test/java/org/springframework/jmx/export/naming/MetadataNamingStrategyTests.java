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
