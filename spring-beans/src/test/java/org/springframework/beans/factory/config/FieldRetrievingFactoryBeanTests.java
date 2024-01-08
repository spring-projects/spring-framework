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

import java.sql.Connection;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Tests for {@link FieldRetrievingFactoryBean}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 31.07.2004
 */
class FieldRetrievingFactoryBeanTests {

	@Test
	void testStaticField() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setStaticField("java.sql.Connection.TRANSACTION_SERIALIZABLE");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void testStaticFieldWithWhitespace() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setStaticField("  java.sql.Connection.TRANSACTION_SERIALIZABLE  ");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void testStaticFieldViaClassAndFieldName() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetClass(Connection.class);
		fr.setTargetField("TRANSACTION_SERIALIZABLE");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void testNonStaticField() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		PublicFieldHolder target = new PublicFieldHolder();
		fr.setTargetObject(target);
		fr.setTargetField("publicField");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(target.publicField);
	}

	@Test
	void testNothingButBeanName() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setBeanName("java.sql.Connection.TRANSACTION_SERIALIZABLE");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void testJustTargetField() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetField("TRANSACTION_SERIALIZABLE");
		try {
			fr.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	void testJustTargetClass() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetClass(Connection.class);
		try {
			fr.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	void testJustTargetObject() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetObject(new PublicFieldHolder());
		try {
			fr.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	void testWithConstantOnClassWithPackageLevelVisibility() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setBeanName("org.springframework.beans.testfixture.beans.PackageLevelVisibleBean.CONSTANT");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo("Wuby");
	}

	@Test
	void testBeanNameSyntaxWithBeanFactory() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(FieldRetrievingFactoryBeanTests.class, "context.xml"));

		TestBean testBean = (TestBean) bf.getBean("testBean");
		assertThat(testBean.getSomeIntegerArray()[0]).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
		assertThat(testBean.getSomeIntegerArray()[1]).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}


	private static class PublicFieldHolder {

		public String publicField = "test";
	}

}
