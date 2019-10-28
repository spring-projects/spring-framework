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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
@Deprecated
public class RequiredAnnotationBeanPostProcessorTests {

	@Test
	public void testWithRequiredPropertyOmitted() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		BeanDefinition beanDef = BeanDefinitionBuilder
			.genericBeanDefinition(RequiredTestBean.class)
			.addPropertyValue("name", "Rob Harrop")
			.addPropertyValue("favouriteColour", "Blue")
			.addPropertyValue("jobTitle", "Grand Poobah")
			.getBeanDefinition();
		factory.registerBeanDefinition("testBean", beanDef);
		factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				factory::preInstantiateSingletons)
			.withMessageContaining("Property")
			.withMessageContaining("age")
			.withMessageContaining("testBean");
	}

	@Test
	public void testWithThreeRequiredPropertiesOmitted() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		BeanDefinition beanDef = BeanDefinitionBuilder
			.genericBeanDefinition(RequiredTestBean.class)
			.addPropertyValue("name", "Rob Harrop")
			.getBeanDefinition();
		factory.registerBeanDefinition("testBean", beanDef);
		factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				factory::preInstantiateSingletons)
			.withMessageContaining("Properties")
			.withMessageContaining("age")
			.withMessageContaining("favouriteColour")
			.withMessageContaining("jobTitle")
			.withMessageContaining("testBean");
	}

	@Test
	public void testWithAllRequiredPropertiesSpecified() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		BeanDefinition beanDef = BeanDefinitionBuilder
			.genericBeanDefinition(RequiredTestBean.class)
			.addPropertyValue("age", "24")
			.addPropertyValue("favouriteColour", "Blue")
			.addPropertyValue("jobTitle", "Grand Poobah")
			.getBeanDefinition();
		factory.registerBeanDefinition("testBean", beanDef);
		factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
		factory.preInstantiateSingletons();
		RequiredTestBean bean = (RequiredTestBean) factory.getBean("testBean");
		assertThat(bean.getAge()).isEqualTo(24);
		assertThat(bean.getFavouriteColour()).isEqualTo("Blue");
	}

	@Test
	public void testWithCustomAnnotation() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		BeanDefinition beanDef = BeanDefinitionBuilder
			.genericBeanDefinition(RequiredTestBean.class)
			.getBeanDefinition();
		factory.registerBeanDefinition("testBean", beanDef);
		RequiredAnnotationBeanPostProcessor rabpp = new RequiredAnnotationBeanPostProcessor();
		rabpp.setRequiredAnnotationType(MyRequired.class);
		factory.addBeanPostProcessor(rabpp);
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				factory::preInstantiateSingletons)
			.withMessageContaining("Property")
			.withMessageContaining("name")
			.withMessageContaining("testBean");
	}

	@Test
	public void testWithStaticFactoryMethod() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		BeanDefinition beanDef = BeanDefinitionBuilder
				.genericBeanDefinition(RequiredTestBean.class)
				.setFactoryMethod("create")
				.addPropertyValue("name", "Rob Harrop")
				.addPropertyValue("favouriteColour", "Blue")
				.addPropertyValue("jobTitle", "Grand Poobah")
				.getBeanDefinition();
		factory.registerBeanDefinition("testBean", beanDef);
		factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
				factory::preInstantiateSingletons)
			.withMessageContaining("Property")
			.withMessageContaining("age")
			.withMessageContaining("testBean");
	}

	@Test
	public void testWithStaticFactoryMethodAndRequiredPropertiesSpecified() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		BeanDefinition beanDef = BeanDefinitionBuilder
				.genericBeanDefinition(RequiredTestBean.class)
				.setFactoryMethod("create")
				.addPropertyValue("age", "24")
				.addPropertyValue("favouriteColour", "Blue")
				.addPropertyValue("jobTitle", "Grand Poobah")
				.getBeanDefinition();
		factory.registerBeanDefinition("testBean", beanDef);
		factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
		factory.preInstantiateSingletons();
		RequiredTestBean bean = (RequiredTestBean) factory.getBean("testBean");
		assertThat(bean.getAge()).isEqualTo(24);
		assertThat(bean.getFavouriteColour()).isEqualTo("Blue");
	}

	@Test
	public void testWithFactoryBean() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition beanDef = new RootBeanDefinition(RequiredTestBean.class);
		beanDef.setFactoryBeanName("testBeanFactory");
		beanDef.setFactoryMethodName("create");
		factory.registerBeanDefinition("testBean", beanDef);
		factory.registerBeanDefinition("testBeanFactory", new RootBeanDefinition(RequiredTestBeanFactory.class));
		RequiredAnnotationBeanPostProcessor bpp = new RequiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(factory);
		factory.addBeanPostProcessor(bpp);
		factory.preInstantiateSingletons();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface MyRequired {
	}


	public static class RequiredTestBean implements BeanNameAware, BeanFactoryAware {

		private String name;

		private int age;

		private String favouriteColour;

		private String jobTitle;


		public int getAge() {
			return age;
		}

		@Required
		public void setAge(int age) {
			this.age = age;
		}

		public String getName() {
			return name;
		}

		@MyRequired
		public void setName(String name) {
			this.name = name;
		}

		public String getFavouriteColour() {
			return favouriteColour;
		}

		@Required
		public void setFavouriteColour(String favouriteColour) {
			this.favouriteColour = favouriteColour;
		}

		public String getJobTitle() {
			return jobTitle;
		}

		@Required
		public void setJobTitle(String jobTitle) {
			this.jobTitle = jobTitle;
		}

		@Override
		@Required
		public void setBeanName(String name) {
		}

		@Override
		@Required
		public void setBeanFactory(BeanFactory beanFactory) {
		}

		public static RequiredTestBean create() {
			return new RequiredTestBean();
		}
	}


	public static class RequiredTestBeanFactory {

		public RequiredTestBean create() {
			return new RequiredTestBean();
		}
	}

}
