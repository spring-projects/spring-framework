/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
import java.util.regex.Pattern;

import example.profilescan.DevComponent;
import example.profilescan.ProfileAnnotatedComponent;
import example.profilescan.ProfileMetaAnnotatedComponent;
import example.scannable.AutowiredQualifierFooService;
import example.scannable.CustomStereotype;
import example.scannable.DefaultNamedComponent;
import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.MessageBean;
import example.scannable.NamedComponent;
import example.scannable.NamedStubDao;
import example.scannable.ScopedProxyTestBean;
import example.scannable.ServiceInvocationCounter;
import example.scannable.StubFooDao;
import example.scannable.sub.BarComponent;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.index.CandidateComponentsTestClassLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
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
 * @author Stephane Nicoll
 */
public class ClassPathScanningCandidateComponentProviderTests {

	private static final String TEST_BASE_PACKAGE = "example.scannable";
	private static final String TEST_PROFILE_PACKAGE = "example.profilescan";
	private static final String TEST_DEFAULT_PROFILE_NAME = "testDefault";

	private static final ClassLoader TEST_BASE_CLASSLOADER = CandidateComponentsTestClassLoader.index(
			ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
			new ClassPathResource("spring.components", NamedComponent.class));


	@Test
	public void defaultsWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testDefault(provider);
	}

	@Test
	public void defaultsWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testDefault(provider);
	}

	private void testDefault(ClassPathScanningCandidateComponentProvider provider) {
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertTrue(containsBeanClass(candidates, DefaultNamedComponent.class));
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, FooServiceImpl.class));
		assertTrue(containsBeanClass(candidates, StubFooDao.class));
		assertTrue(containsBeanClass(candidates, NamedStubDao.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, BarComponent.class));
		assertEquals(7, candidates.size());
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void antStylePackageWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testAntStyle(provider);
	}

	@Test
	public void antStylePackageWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testAntStyle(provider);
	}

	private void testAntStyle(ClassPathScanningCandidateComponentProvider provider) {
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE + ".**.sub");
		assertTrue(containsBeanClass(candidates, BarComponent.class));
		assertEquals(1, candidates.size());
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void bogusPackageWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		Set<BeanDefinition> candidates = provider.findCandidateComponents("bogus");
		assertEquals(0, candidates.size());
	}

	@Test
	public void bogusPackageWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		Set<BeanDefinition> candidates = provider.findCandidateComponents("bogus");
		assertEquals(0, candidates.size());
	}

	@Test
	public void customFiltersFollowedByResetUseIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.resetFilters(true);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void customAnnotationTypeIncludeFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testCustomAnnotationTypeIncludeFilter(provider);
	}

	@Test
	public void customAnnotationTypeIncludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testCustomAnnotationTypeIncludeFilter(provider);
	}

	private void testCustomAnnotationTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		testDefault(provider);
	}

	@Test
	public void customAssignableTypeIncludeFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testCustomAssignableTypeIncludeFilter(provider);
	}

	@Test
	public void customAssignableTypeIncludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testCustomAssignableTypeIncludeFilter(provider);
	}

	private void testCustomAssignableTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
		provider.addIncludeFilter(new AssignableTypeFilter(FooService.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		// Interfaces/Abstract class are filtered out automatically.
		assertTrue(containsBeanClass(candidates, AutowiredQualifierFooService.class));
		assertTrue(containsBeanClass(candidates, FooServiceImpl.class));
		assertTrue(containsBeanClass(candidates, ScopedProxyTestBean.class));
		assertEquals(3, candidates.size());
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void customSupportedIncludeAndExcludedFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testCustomSupportedIncludeAndExcludeFilter(provider);
	}

	@Test
	public void customSupportedIncludeAndExcludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testCustomSupportedIncludeAndExcludeFilter(provider);
	}

	private void testCustomSupportedIncludeAndExcludeFilter(ClassPathScanningCandidateComponentProvider provider) {
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, BarComponent.class));
		assertEquals(3, candidates.size());
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void customSupportIncludeFilterWithNonIndexedTypeUseScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		// This annotation type is not directly annotated with Indexed so we can use
		// the index to find candidates
		provider.addIncludeFilter(new AnnotationTypeFilter(CustomStereotype.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertTrue(containsBeanClass(candidates, DefaultNamedComponent.class));
		assertEquals(1, candidates.size());
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void customNotSupportedIncludeFilterUseScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertTrue(containsBeanClass(candidates, StubFooDao.class));
		assertEquals(1, candidates.size());
		assertBeanDefinitionType(candidates);
	}

	@Test
	public void excludeFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
		testExclude(provider);
	}

	@Test
	public void excludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
		testExclude(provider);
	}

	private void testExclude(ClassPathScanningCandidateComponentProvider provider) {
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertTrue(containsBeanClass(candidates, FooServiceImpl.class));
		assertTrue(containsBeanClass(candidates, StubFooDao.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, BarComponent.class));
		assertEquals(4, candidates.size());
		assertBeanDefinitionType(candidates);
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
		assertEquals(3, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, BarComponent.class));
		assertFalse(containsBeanClass(candidates, FooServiceImpl.class));
		assertFalse(containsBeanClass(candidates, StubFooDao.class));
		assertFalse(containsBeanClass(candidates, NamedStubDao.class));
	}

	@Test
	public void testWithAspectAnnotationOnly() {
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
		assertEquals(7, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, FooServiceImpl.class));
		assertTrue(containsBeanClass(candidates, BarComponent.class));
	}

	@Test
	public void testExcludeTakesPrecedence() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
		provider.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertEquals(6, candidates.size());
		assertTrue(containsBeanClass(candidates, NamedComponent.class));
		assertTrue(containsBeanClass(candidates, ServiceInvocationCounter.class));
		assertTrue(containsBeanClass(candidates, BarComponent.class));
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
			if (beanClass.getName().equals(candidate.getBeanClassName())) {
				return true;
			}
		}
		return false;
	}

	private void assertBeanDefinitionType(Set<BeanDefinition> candidates) {
		candidates.forEach(c -> {
			assertThat(c, is(instanceOf(ScannedGenericBeanDefinition.class)));
		});
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
