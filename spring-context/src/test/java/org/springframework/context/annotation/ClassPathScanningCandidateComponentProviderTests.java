/*
 * Copyright 2002-2010 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.regex.Pattern;

import example.profilescan.DevComponent;
import example.profilescan.ProfileAnnotatedComponent;
import example.profilescan.ProfileMetaAnnotatedComponent;
import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.MessageBean;
import example.scannable.NamedComponent;
import example.scannable.NamedStubDao;
import example.scannable.ServiceInvocationCounter;
import example.scannable.StubFooDao;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ClassPathScanningCandidateComponentProviderTests {

	private static final String TEST_BASE_PACKAGE = "example.scannable";
	private static final String TEST_PROFILE_PACKAGE = "example.profilescan";
	private static final String TEST_DEFAULT_PROFILE_NAME = "testDefault";


	@Test
	public void testWithDefaults() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(6, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, FooServiceImpl.class));
		assertTrue(containsBeanClass(candidates, StubFooDao.class));
		assertTrue(containsBeanClass(candidates, NamedStubDao.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
	}

	@Test
	public void testWithBogusBasePackage() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		Set<BeanDefinition> candidates = provider.findCandidateComponents("bogus");
		assertEquals(0, candidates.size());
	}

	@Test
	public void testWithPackageExcludeFilter() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*")));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(0, candidates.size());
	}

	@Test
	public void testWithNoFilters() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(0, candidates.size());
	}

	@Test
	public void testWithComponentAnnotationOnly() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(2, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertFalse(containsBeanClass(candidates, FooServiceImpl.class));
		assertFalse(containsBeanClass(candidates, StubFooDao.class));
		assertFalse(containsBeanClass(candidates, NamedStubDao.class));
	}

	@Test
	public void testWithAspectAnnotationOnly() throws Exception {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Aspect.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(1, candidates.size());
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
	}

	@Test
	public void testWithInterfaceType() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(1, candidates.size());
		assertTrue(containsBeanClass(candidates, StubFooDao.class));
	}

	@Test
	public void testWithClassType() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(MessageBean.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(1, candidates.size());
		assertTrue(containsBeanClass(candidates, MessageBean.class));
	}

	@Test
	public void testWithMultipleMatchingFilters() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(6, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, FooServiceImpl.class));
	}

	@Test
	public void testExcludeTakesPrecedence() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
		provider.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(5, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertFalse(containsBeanClass(candidates, FooServiceImpl.class));
	}

	@Test
	public void testWithNullEnvironment() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
		assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class), is(false));
	}

	@Test
	public void testWithInactiveProfile() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		ConfigurableEnvironment env = new StandardEnvironment();
		env.setActiveProfiles("other");
		provider.setEnvironment(env);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
		assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class), is(false));
	}

	@Test
	public void testWithActiveProfile() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		ConfigurableEnvironment env = new StandardEnvironment();
		env.setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
		provider.setEnvironment(env);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
		assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class), is(true));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_noProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME), is(false));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_validProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
		ctx.register(ProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME), is(true));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_validMetaAnnotatedProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles(DevComponent.PROFILE_NAME);
		ctx.register(ProfileMetaAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME), is(true));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_invalidProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles("other");
		ctx.register(ProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME), is(false));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_invalidMetaAnnotatedProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles("other");
		ctx.register(ProfileMetaAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME), is(false));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_defaultProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
		// no active profiles are set
		ctx.register(DefaultProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(DefaultProfileAnnotatedComponent.BEAN_NAME), is(true));
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_defaultAndDevProfile() {
		Class<?> beanClass = DefaultAndDevProfileAnnotatedComponent.class;
		String beanName = DefaultAndDevProfileAnnotatedComponent.BEAN_NAME;
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			// no active profiles are set
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName), is(true));
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("dev");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName), is(true));
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("other");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName), is(false));
		}
	}

	@Test
	public void testIntegrationWithAnnotationConfigApplicationContext_metaProfile() {
		Class<?> beanClass = MetaProfileAnnotatedComponent.class;
		String beanName = MetaProfileAnnotatedComponent.BEAN_NAME;
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			// no active profiles are set
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName), is(true));
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("dev");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName), is(true));
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("other");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName), is(false));
		}
	}

	private boolean containsBeanClass(Set<BeanDefinition> candidates, Class<?> beanClass) {
		for (BeanDefinition candidate : candidates) {
			ScannedGenericBeanDefinition definition = (ScannedGenericBeanDefinition) candidate;
			if (beanClass.getName().equals(definition.getBeanClassName())) {
				return true;
			}
		}
		return false;
	}


	@Profile(TEST_DEFAULT_PROFILE_NAME)
	@Component(DefaultProfileAnnotatedComponent.BEAN_NAME)
	private static class DefaultProfileAnnotatedComponent {
		static final String BEAN_NAME = "defaultProfileAnnotatedComponent";
	}

	@Profile({TEST_DEFAULT_PROFILE_NAME, "dev"})
	@Component(DefaultAndDevProfileAnnotatedComponent.BEAN_NAME)
	private static class DefaultAndDevProfileAnnotatedComponent {
		static final String BEAN_NAME = "defaultAndDevProfileAnnotatedComponent";
	}

	@DefaultProfile @DevProfile
	@Component(MetaProfileAnnotatedComponent.BEAN_NAME)
	private static class MetaProfileAnnotatedComponent {
		static final String BEAN_NAME = "metaProfileAnnotatedComponent";
	}

	@Profile(TEST_DEFAULT_PROFILE_NAME)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DefaultProfile {
	}

	@Profile("dev")
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DevProfile {
	}

}
