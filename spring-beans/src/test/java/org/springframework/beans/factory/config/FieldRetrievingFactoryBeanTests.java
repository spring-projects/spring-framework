/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void staticField() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setStaticField("java.sql.Connection.TRANSACTION_SERIALIZABLE");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void staticFieldWithWhitespace() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setStaticField("  java.sql.Connection.TRANSACTION_SERIALIZABLE  ");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void staticFieldViaClassAndFieldName() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetClass(Connection.class);
		fr.setTargetField("TRANSACTION_SERIALIZABLE");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void nonStaticField() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		PublicFieldHolder target = new PublicFieldHolder();
		fr.setTargetObject(target);
		fr.setTargetField("publicField");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(target.publicField);
	}

	@Test
	void nothingButBeanName() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setBeanName("java.sql.Connection.TRANSACTION_SERIALIZABLE");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo(Connection.TRANSACTION_SERIALIZABLE);
	}

	@Test
	void justTargetField() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetField("TRANSACTION_SERIALIZABLE");
		try {
			fr.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	void justTargetClass() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetClass(Connection.class);
		try {
			fr.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	void justTargetObject() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setTargetObject(new PublicFieldHolder());
		try {
			fr.afterPropertiesSet();
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	void withConstantOnClassWithPackageLevelVisibility() throws Exception {
		FieldRetrievingFactoryBean fr = new FieldRetrievingFactoryBean();
		fr.setBeanName("org.springframework.beans.testfixture.beans.PackageLevelVisibleBean.CONSTANT");
		fr.afterPropertiesSet();
		assertThat(fr.getObject()).isEqualTo("Wuby");
	}

	@Test
	void beanNameSyntaxWithBeanFactory() {
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
