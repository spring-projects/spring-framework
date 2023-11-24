/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link XmlBeanDefinitionReader}.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class XmlBeanDefinitionReaderTests {

	private final SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);


	@Test
	void setReaderClass() {
		assertThatNoException().isThrownBy(() -> reader.setDocumentReaderClass(DefaultBeanDefinitionDocumentReader.class));
	}

	@Test
	void withInputStreamResourceWithoutExplicitValidationMode() {
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() -> reader.loadBeanDefinitions(resource));
	}

	@Test
	void withInputStreamResourceAndExplicitValidationMode() {
		Resource resource = new InputStreamResource(getClass().getResourceAsStream("test.xml"));
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		assertBeanDefinitions(registry);
	}

	@Test
	void withImport() {
		Resource resource = new ClassPathResource("import.xml", getClass());
		reader.loadBeanDefinitions(resource);
		assertBeanDefinitions(registry);
	}

	@Test
	void withWildcardImport() {
		Resource resource = new ClassPathResource("importPattern.xml", getClass());
		reader.loadBeanDefinitions(resource);
		assertBeanDefinitions(registry);
	}

	@Test
	void withInputSourceWithoutExplicitValidationMode() {
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> reader.loadBeanDefinitions(resource))
				.withMessageStartingWith("Unable to determine validation mode for [resource loaded through SAX InputSource]:");
	}

	@Test
	void withInputSourceAndExplicitValidationMode() {
		InputSource resource = new InputSource(getClass().getResourceAsStream("test.xml"));
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD);
		reader.loadBeanDefinitions(resource);
		assertBeanDefinitions(registry);
	}

	@Test
	void withClassPathResource() {
		Resource resource = new ClassPathResource("test.xml", getClass());
		reader.loadBeanDefinitions(resource);
		assertBeanDefinitions(registry);
	}

	private void assertBeanDefinitions(BeanDefinitionRegistry registry) {
		assertThat(registry.getBeanDefinitionCount()).isEqualTo(24);
		assertThat(registry.getBeanDefinitionNames()).hasSize(24);
		assertThat(registry.getBeanDefinitionNames()).contains("rod", "aliased");
		assertThat(registry.containsBeanDefinition("rod")).isTrue();
		assertThat(registry.containsBeanDefinition("aliased")).isTrue();
		assertThat(registry.getBeanDefinition("rod").getBeanClassName()).isEqualTo(TestBean.class.getName());
		assertThat(registry.getBeanDefinition("aliased").getBeanClassName()).isEqualTo(TestBean.class.getName());
		assertThat(registry.isAlias("youralias")).isTrue();
		assertThat(registry.getAliases("aliased")).containsExactly("myalias", "youralias");
	}

	@Test
	void dtdValidationAutodetect() {
		doTestValidation("validateWithDtd.xml");
	}

	@Test
	void xsdValidationAutodetect() {
		doTestValidation("validateWithXsd.xml");
	}

	private void doTestValidation(String resourceName) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		Resource resource = new ClassPathResource(resourceName, getClass());
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(resource);
		assertThat((TestBean) factory.getBean("testBean")).isNotNull();
	}

	@Test
	void setValidationModeNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setValidationModeName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setValidationModeName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setValidationModeName("bogus"));
	}

	/**
	 * This test effectively verifies that the internal 'constants' map is properly
	 * configured for all VALIDATION_ constants defined in {@link XmlBeanDefinitionReader}.
	 */
	@Test
	void setValidationModeNameToAllSupportedValues() {
		streamValidationModeConstants()
				.map(Field::getName)
				.forEach(name -> assertThatNoException().as(name).isThrownBy(() -> reader.setValidationModeName(name)));
	}

	@Test
	void setValidationMode() {
		assertThatIllegalArgumentException().isThrownBy(() -> reader.setValidationMode(999));

		assertThatNoException().isThrownBy(() -> reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE));
		assertThatNoException().isThrownBy(() -> reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_AUTO));
		assertThatNoException().isThrownBy(() -> reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_DTD));
		assertThatNoException().isThrownBy(() -> reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD));
	}


	private static Stream<Field> streamValidationModeConstants() {
		return Arrays.stream(XmlBeanDefinitionReader.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.filter(field -> field.getName().startsWith("VALIDATION_"));
	}

}
