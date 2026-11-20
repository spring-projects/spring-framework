/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.util.Date;

import jakarta.inject.Provider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 */
class ObjectFactoryCreatingFactoryBeanTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	void setup() {
		this.beanFactory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				qualifiedResource(ObjectFactoryCreatingFactoryBeanTests.class, "context.xml"));
		this.beanFactory.setSerializationId("test");
	}

	@AfterEach
	void close() {
		this.beanFactory.setSerializationId(null);
	}


	@Test
	void factoryOperation() {
		FactoryTestBean testBean = beanFactory.getBean("factoryTestBean", FactoryTestBean.class);
		ObjectFactory<?> objectFactory = testBean.getObjectFactory();

		Date date1 = (Date) objectFactory.getObject();
		Date date2 = (Date) objectFactory.getObject();
		assertThat(date1).isNotSameAs(date2);
	}

	@Test
	void factorySerialization() throws Exception {
		FactoryTestBean testBean = beanFactory.getBean("factoryTestBean", FactoryTestBean.class);
		ObjectFactory<?> objectFactory = testBean.getObjectFactory();

		objectFactory = SerializationTestUtils.serializeAndDeserialize(objectFactory);

		Date date1 = (Date) objectFactory.getObject();
		Date date2 = (Date) objectFactory.getObject();
		assertThat(date1).isNotSameAs(date2);
	}

	@Test
	void providerOperation() {
		ProviderTestBean testBean = beanFactory.getBean("providerTestBean", ProviderTestBean.class);
		Provider<?> provider = testBean.getProvider();

		Date date1 = (Date) provider.get();
		Date date2 = (Date) provider.get();
		assertThat(date1).isNotSameAs(date2);
	}

	@Test
	void providerSerialization() throws Exception {
		ProviderTestBean testBean = beanFactory.getBean("providerTestBean", ProviderTestBean.class);
		Provider<?> provider = testBean.getProvider();

		provider = SerializationTestUtils.serializeAndDeserialize(provider);

		Date date1 = (Date) provider.get();
		Date date2 = (Date) provider.get();
		assertThat(date1).isNotSameAs(date2);
	}

	@Test
	void doesNotComplainWhenTargetBeanNameRefersToSingleton() throws Exception {
		final String targetBeanName = "singleton";
		final String expectedSingleton = "Alicia Keys";

		BeanFactory beanFactory = mock();
		given(beanFactory.getBean(targetBeanName)).willReturn(expectedSingleton);

		ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
		factory.setTargetBeanName(targetBeanName);
		factory.setBeanFactory(beanFactory);
		factory.afterPropertiesSet();
		ObjectFactory<?> objectFactory = factory.getObject();
		Object actualSingleton = objectFactory.getObject();
		assertThat(actualSingleton).isSameAs(expectedSingleton);
	}

	@Test
	void whenTargetBeanNameIsNull() {
		assertThatIllegalArgumentException().as(
				"'targetBeanName' property not set").isThrownBy(
						new ObjectFactoryCreatingFactoryBean()::afterPropertiesSet);
	}

	@Test
	void whenTargetBeanNameIsEmptyString() {
		ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
		factory.setTargetBeanName("");
		assertThatIllegalArgumentException().as(
				"'targetBeanName' property set to (invalid) empty string").isThrownBy(
						factory::afterPropertiesSet);
	}

	@Test
	void whenTargetBeanNameIsWhitespacedString() {
		ObjectFactoryCreatingFactoryBean factory = new ObjectFactoryCreatingFactoryBean();
		factory.setTargetBeanName("  \t");
		assertThatIllegalArgumentException().as(
				"'targetBeanName' property set to (invalid) only-whitespace string").isThrownBy(
						factory::afterPropertiesSet);
	}

	@Test
	void ensureOFBFBReportsThatItActuallyCreatesObjectFactoryInstances() {
		assertThat(new ObjectFactoryCreatingFactoryBean().getObjectType()).as("Must be reporting that it creates ObjectFactory instances (as per class contract).").isEqualTo(ObjectFactory.class);
	}


	public static class FactoryTestBean {

		private ObjectFactory<?> objectFactory;

		public ObjectFactory<?> getObjectFactory() {
			return objectFactory;
		}

		public void setObjectFactory(ObjectFactory<?> objectFactory) {
			this.objectFactory = objectFactory;
		}
	}


	public static class ProviderTestBean {

		private Provider<?> provider;

		public Provider<?> getProvider() {
			return provider;
		}

		public void setProvider(Provider<?> provider) {
			this.provider = provider;
		}
	}

}
