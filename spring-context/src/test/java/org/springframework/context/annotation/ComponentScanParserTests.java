/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import example.profilescan.ProfileAnnotatedComponent;
import example.scannable.AutowiredQualifierFooService;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
public class ComponentScanParserTests {

	private ClassPathXmlApplicationContext loadContext(String path) {
		return new ClassPathXmlApplicationContext(path, getClass());
	}

	@Test
	public void aspectJTypeFilter() {
		ClassPathXmlApplicationContext context = loadContext("aspectjTypeFilterTests.xml");
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertFalse(context.containsBean("scopedProxyTestBean"));
		context.close();
	}

	@Test
	public void nonMatchingResourcePattern() {
		ClassPathXmlApplicationContext context = loadContext("nonMatchingResourcePatternTests.xml");
		assertFalse(context.containsBean("fooServiceImpl"));
		context.close();
	}

	@Test
	public void matchingResourcePattern() {
		ClassPathXmlApplicationContext context = loadContext("matchingResourcePatternTests.xml");
		assertTrue(context.containsBean("fooServiceImpl"));
		context.close();
	}

	@Test
	public void componentScanWithAutowiredQualifier() {
		ClassPathXmlApplicationContext context = loadContext("componentScanWithAutowiredQualifierTests.xml");
		AutowiredQualifierFooService fooService = (AutowiredQualifierFooService) context.getBean("fooService");
		assertTrue(fooService.isInitCalled());
		assertEquals("bar", fooService.foo(123));
		context.close();
	}

	@Test
	public void customAnnotationUsedForBothComponentScanAndQualifier() {
		ClassPathXmlApplicationContext context = loadContext("customAnnotationUsedForBothComponentScanAndQualifierTests.xml");
		KustomAnnotationAutowiredBean testBean = (KustomAnnotationAutowiredBean) context.getBean("testBean");
		assertNotNull(testBean.getDependency());
		context.close();
	}

	@Test
	public void customTypeFilter() {
		ClassPathXmlApplicationContext context = loadContext("customTypeFilterTests.xml");
		KustomAnnotationAutowiredBean testBean = (KustomAnnotationAutowiredBean) context.getBean("testBean");
		assertNotNull(testBean.getDependency());
		context.close();
	}

	@Test
	public void componentScanRespectsProfileAnnotation() {
		String xmlLocation = "org/springframework/context/annotation/componentScanRespectsProfileAnnotationTests.xml";
		{ // should exclude the profile-annotated bean if active profiles remains unset
			GenericXmlApplicationContext context = new GenericXmlApplicationContext();
			context.load(xmlLocation);
			context.refresh();
			assertThat(context.containsBean(ProfileAnnotatedComponent.BEAN_NAME), is(false));
			context.close();
		}
		{ // should include the profile-annotated bean with active profiles set
			GenericXmlApplicationContext context = new GenericXmlApplicationContext();
			context.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
			context.load(xmlLocation);
			context.refresh();
			assertThat(context.containsBean(ProfileAnnotatedComponent.BEAN_NAME), is(true));
			context.close();
		}
		{ // ensure the same works for AbstractRefreshableApplicationContext impls too
			ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(new String[] { xmlLocation },
				false);
			context.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
			context.refresh();
			assertThat(context.containsBean(ProfileAnnotatedComponent.BEAN_NAME), is(true));
			context.close();
		}
	}


	@Target({ ElementType.TYPE, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface CustomAnnotation {
	}

	/**
	 * Intentionally spelling "custom" with a "k" since there are numerous
	 * classes in this package named *Custom*.
	 */
	public static class KustomAnnotationAutowiredBean {

		@Autowired
		@CustomAnnotation
		private KustomAnnotationDependencyBean dependency;


		public KustomAnnotationDependencyBean getDependency() {
			return this.dependency;
		}
	}

	/**
	 * Intentionally spelling "custom" with a "k" since there are numerous
	 * classes in this package named *Custom*.
	 */
	@CustomAnnotation
	public static class KustomAnnotationDependencyBean {
	}

	public static class CustomTypeFilter implements TypeFilter {

		/**
		 * Intentionally spelling "custom" with a "k" since there are numerous
		 * classes in this package named *Custom*.
		 */
		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
			return metadataReader.getClassMetadata().getClassName().contains("Kustom");
		}
	}

}
