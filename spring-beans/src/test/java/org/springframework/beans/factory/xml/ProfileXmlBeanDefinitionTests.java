/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests various combinations of profile declarations against various profile
 * activation and profile default scenarios.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.1
 */
public class ProfileXmlBeanDefinitionTests {

	private static final String PROD_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-prodProfile.xml";
	private static final String DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-devProfile.xml";
	private static final String NOT_DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-notDevProfile.xml";
	private static final String ALL_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-noProfile.xml";
	private static final String MULTI_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-multiProfile.xml";
	private static final String MULTI_NEGATED_XML = "ProfileXmlBeanDefinitionTests-multiProfileNegated.xml";
	private static final String MULTI_NOT_DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-multiProfileNotDev.xml";
	private static final String MULTI_ELIGIBLE_SPACE_DELIMITED_XML = "ProfileXmlBeanDefinitionTests-spaceDelimitedProfile.xml";
	private static final String UNKNOWN_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-unknownProfile.xml";
	private static final String DEFAULT_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-defaultProfile.xml";
	private static final String CUSTOM_DEFAULT_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-customDefaultProfile.xml";
	private static final String DEFAULT_AND_DEV_ELIGIBLE_XML = "ProfileXmlBeanDefinitionTests-defaultAndDevProfile.xml";

	private static final String PROD_ACTIVE = "prod";
	private static final String DEV_ACTIVE = "dev";
	private static final String NULL_ACTIVE = null;
	private static final String UNKNOWN_ACTIVE = "unknown";
	private static final String[] NONE_ACTIVE = new String[0];
	private static final String[] MULTI_ACTIVE = new String[] { PROD_ACTIVE, DEV_ACTIVE };

	private static final String TARGET_BEAN = "foo";

	@Test(expected = IllegalArgumentException.class)
	public void testProfileValidation() {
		beanFactoryFor(PROD_ELIGIBLE_XML, NULL_ACTIVE);
	}

	@Test
	public void testProfilePermutations() {
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, DEV_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(PROD_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, PROD_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(DEV_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, NONE_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, DEV_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(NOT_DEV_ELIGIBLE_XML, MULTI_ACTIVE), not(containsTargetBean()));

		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, NONE_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(ALL_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, UNKNOWN_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(MULTI_NEGATED_XML, NONE_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NEGATED_XML, UNKNOWN_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NEGATED_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NEGATED_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NEGATED_XML, MULTI_ACTIVE), not(containsTargetBean()));

		assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, NONE_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, UNKNOWN_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, DEV_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_NOT_DEV_ELIGIBLE_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, NONE_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, UNKNOWN_ACTIVE), not(containsTargetBean()));
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, DEV_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, PROD_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(MULTI_ELIGIBLE_SPACE_DELIMITED_XML, MULTI_ACTIVE), containsTargetBean());

		assertThat(beanFactoryFor(UNKNOWN_ELIGIBLE_XML, MULTI_ACTIVE), not(containsTargetBean()));
	}

	@Test
	public void testDefaultProfile() {
		{
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
			ConfigurableEnvironment env = new StandardEnvironment();
			env.setDefaultProfiles("custom-default");
			reader.setEnvironment(env);
			reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_ELIGIBLE_XML, getClass()));

			assertThat(beanFactory, not(containsTargetBean()));
		}
		{
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
			ConfigurableEnvironment env = new StandardEnvironment();
			env.setDefaultProfiles("custom-default");
			reader.setEnvironment(env);
			reader.loadBeanDefinitions(new ClassPathResource(CUSTOM_DEFAULT_ELIGIBLE_XML, getClass()));

			assertThat(beanFactory, containsTargetBean());
		}
	}

	@Test
	public void testDefaultAndNonDefaultProfile() {
		assertThat(beanFactoryFor(DEFAULT_ELIGIBLE_XML, NONE_ACTIVE), containsTargetBean());
		assertThat(beanFactoryFor(DEFAULT_ELIGIBLE_XML, "other"), not(containsTargetBean()));

		{
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
			ConfigurableEnvironment env = new StandardEnvironment();
			env.setActiveProfiles(DEV_ACTIVE);
			env.setDefaultProfiles("default");
			reader.setEnvironment(env);
			reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_AND_DEV_ELIGIBLE_XML, getClass()));
			assertThat(beanFactory, containsTargetBean());
		}
		{
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
			ConfigurableEnvironment env = new StandardEnvironment();
			// env.setActiveProfiles(DEV_ACTIVE);
			env.setDefaultProfiles("default");
			reader.setEnvironment(env);
			reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_AND_DEV_ELIGIBLE_XML, getClass()));
			assertThat(beanFactory, containsTargetBean());
		}
		{
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
			ConfigurableEnvironment env = new StandardEnvironment();
			// env.setActiveProfiles(DEV_ACTIVE);
			//env.setDefaultProfiles("default");
			reader.setEnvironment(env);
			reader.loadBeanDefinitions(new ClassPathResource(DEFAULT_AND_DEV_ELIGIBLE_XML, getClass()));
			assertThat(beanFactory, containsTargetBean());
		}
	}


	private BeanDefinitionRegistry beanFactoryFor(String xmlName, String... activeProfiles) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		StandardEnvironment env = new StandardEnvironment();
		env.setActiveProfiles(activeProfiles);
		reader.setEnvironment(env);
		reader.loadBeanDefinitions(new ClassPathResource(xmlName, getClass()));
		return beanFactory;
	}


	private static Matcher<BeanDefinitionRegistry> containsBeanDefinition(final String beanName) {
		return new TypeSafeMatcher<BeanDefinitionRegistry>() {

			@Override
			public void describeTo(Description desc) {
				desc.appendText("a BeanDefinitionRegistry containing bean named ")
					.appendValue(beanName);
			}

			@Override
			public boolean matchesSafely(BeanDefinitionRegistry beanFactory) {
				return beanFactory.containsBeanDefinition(beanName);
			}

		};
	}

	private static Matcher<BeanDefinitionRegistry> containsTargetBean() {
		return containsBeanDefinition(TARGET_BEAN);
	}
}
