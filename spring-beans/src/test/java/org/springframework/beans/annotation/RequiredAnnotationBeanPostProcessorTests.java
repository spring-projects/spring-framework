/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.annotation;

import static org.junit.Assert.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @since 2.0
 */
public final class RequiredAnnotationBeanPostProcessorTests {

	@Test
	public void testWithRequiredPropertyOmitted() {
		try {
			DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
			BeanDefinition beanDef = BeanDefinitionBuilder
				.genericBeanDefinition(RequiredTestBean.class)
				.addPropertyValue("name", "Rob Harrop")
				.addPropertyValue("favouriteColour", "Blue")
				.addPropertyValue("jobTitle", "Grand Poobah")
				.getBeanDefinition();
			factory.registerBeanDefinition("testBean", beanDef);
			factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
			factory.preInstantiateSingletons();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			String message = ex.getCause().getMessage();
			assertTrue(message.indexOf("Property") > -1);
			assertTrue(message.indexOf("age") > -1);
			assertTrue(message.indexOf("testBean") > -1);
		}
	}

	@Test
	public void testWithThreeRequiredPropertiesOmitted() {
		try {
			DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
			BeanDefinition beanDef = BeanDefinitionBuilder
				.genericBeanDefinition(RequiredTestBean.class)
				.addPropertyValue("name", "Rob Harrop")
				.getBeanDefinition();
			factory.registerBeanDefinition("testBean", beanDef);
			factory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
			factory.preInstantiateSingletons();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			String message = ex.getCause().getMessage();
			assertTrue(message.indexOf("Properties") > -1);
			assertTrue(message.indexOf("age") > -1);
			assertTrue(message.indexOf("favouriteColour") > -1);
			assertTrue(message.indexOf("jobTitle") > -1);
			assertTrue(message.indexOf("testBean") > -1);
		}
	}

	@Test
	public void testWithOnlyRequiredPropertiesSpecified() {
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
		assertEquals(24, bean.getAge());
		assertEquals("Blue", bean.getFavouriteColour());
	}

	@Test
	public void testWithCustomAnnotation() {
		try {
			DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
			BeanDefinition beanDef = BeanDefinitionBuilder
				.genericBeanDefinition(RequiredTestBean.class)
				.getBeanDefinition();
			factory.registerBeanDefinition("testBean", beanDef);
			RequiredAnnotationBeanPostProcessor rabpp = new RequiredAnnotationBeanPostProcessor();
			rabpp.setRequiredAnnotationType(MyRequired.class);
			factory.addBeanPostProcessor(rabpp);
			factory.preInstantiateSingletons();
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			String message = ex.getCause().getMessage();
			assertTrue(message.indexOf("Property") > -1);
			assertTrue(message.indexOf("name") > -1);
			assertTrue(message.indexOf("testBean") > -1);
		}
	}

}


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface MyRequired {
}


class RequiredTestBean implements BeanNameAware, BeanFactoryAware {

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

	@Required
	public void setBeanName(String name) {
	}

	@Required
	public void setBeanFactory(BeanFactory beanFactory) {
	}

}
