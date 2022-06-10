/*
 * Copyright 2002-2022 the original author or authors.
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

import example.gh24375.AnnotatedComponent;
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
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.testfixture.index.CandidateComponentsTestClassLoader;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Stephane Nicoll
 */
class ClassPathScanningCandidateComponentProviderTests {

	private static final String TEST_BASE_PACKAGE = "example.scannable";
	private static final String TEST_PROFILE_PACKAGE = "example.profilescan";
	private static final String TEST_DEFAULT_PROFILE_NAME = "testDefault";

	private static final ClassLoader TEST_BASE_CLASSLOADER = CandidateComponentsTestClassLoader.index(
			ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
			new ClassPathResource("spring.components", NamedComponent.class));


	@Test
	void defaultsWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testDefault(provider);
	}

	@Test
	void defaultsWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testDefault(provider);
	}

	private void testDefault(ClassPathScanningCandidateComponentProvider provider) {
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(containsBeanClass(candidates, DefaultNamedComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
		assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
		assertThat(containsBeanClass(candidates, NamedStubDao.class)).isTrue();
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(7);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void antStylePackageWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testAntStyle(provider);
	}

	@Test
	void antStylePackageWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testAntStyle(provider);
	}

	private void testAntStyle(ClassPathScanningCandidateComponentProvider provider) {
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE + ".**.sub");
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(1);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void bogusPackageWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		Set<BeanDefinition> candidates = provider.findCandidateComponents("bogus");
		assertThat(candidates.size()).isEqualTo(0);
	}

	@Test
	void bogusPackageWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		Set<BeanDefinition> candidates = provider.findCandidateComponents("bogus");
		assertThat(candidates.size()).isEqualTo(0);
	}

	@Test
	void customFiltersFollowedByResetUseIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.resetFilters(true);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void customAnnotationTypeIncludeFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testCustomAnnotationTypeIncludeFilter(provider);
	}

	@Test
	void customAnnotationTypeIncludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testCustomAnnotationTypeIncludeFilter(provider);
	}

	private void testCustomAnnotationTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		testDefault(provider);
	}

	@Test
	void customAssignableTypeIncludeFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testCustomAssignableTypeIncludeFilter(provider);
	}

	@Test
	void customAssignableTypeIncludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testCustomAssignableTypeIncludeFilter(provider);
	}

	private void testCustomAssignableTypeIncludeFilter(ClassPathScanningCandidateComponentProvider provider) {
		provider.addIncludeFilter(new AssignableTypeFilter(FooService.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		// Interfaces/Abstract class are filtered out automatically.
		assertThat(containsBeanClass(candidates, AutowiredQualifierFooService.class)).isTrue();
		assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
		assertThat(containsBeanClass(candidates, ScopedProxyTestBean.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(3);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void customSupportedIncludeAndExcludedFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		testCustomSupportedIncludeAndExcludeFilter(provider);
	}

	@Test
	void customSupportedIncludeAndExcludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		testCustomSupportedIncludeAndExcludeFilter(provider);
	}

	private void testCustomSupportedIncludeAndExcludeFilter(ClassPathScanningCandidateComponentProvider provider) {
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(3);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void customSupportIncludeFilterWithNonIndexedTypeUseScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		// This annotation type is not directly annotated with Indexed so we can use
		// the index to find candidates
		provider.addIncludeFilter(new AnnotationTypeFilter(CustomStereotype.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(containsBeanClass(candidates, DefaultNamedComponent.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(1);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void customNotSupportedIncludeFilterUseScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(1);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void excludeFilterWithScan() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(
				CandidateComponentsTestClassLoader.disableIndex(getClass().getClassLoader())));
		provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
		testExclude(provider);
	}

	@Test
	void excludeFilterWithIndex() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		provider.setResourceLoader(new DefaultResourceLoader(TEST_BASE_CLASSLOADER));
		provider.addExcludeFilter(new RegexPatternTypeFilter(Pattern.compile(TEST_BASE_PACKAGE + ".*Named.*")));
		testExclude(provider);
	}

	private void testExclude(ClassPathScanningCandidateComponentProvider provider) {
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
		assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
		assertThat(candidates.size()).isEqualTo(4);
		assertBeanDefinitionType(candidates);
	}

	@Test
	void withNoFilters() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(0);
	}

	@Test
	void withComponentAnnotationOnly() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Repository.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Service.class));
		provider.addExcludeFilter(new AnnotationTypeFilter(Controller.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(3);
		assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isFalse();
		assertThat(containsBeanClass(candidates, StubFooDao.class)).isFalse();
		assertThat(containsBeanClass(candidates, NamedStubDao.class)).isFalse();
	}

	@Test
	void withAspectAnnotationOnly() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Aspect.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(1);
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
	}

	@Test
	void withInterfaceType() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(FooDao.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(1);
		assertThat(containsBeanClass(candidates, StubFooDao.class)).isTrue();
	}

	@Test
	void withClassType() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AssignableTypeFilter(MessageBean.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(1);
		assertThat(containsBeanClass(candidates, MessageBean.class)).isTrue();
	}

	@Test
	void withMultipleMatchingFilters() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(7);
		assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
		assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isTrue();
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
	}

	@Test
	void excludeTakesPrecedence() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Component.class));
		provider.addIncludeFilter(new AssignableTypeFilter(FooServiceImpl.class));
		provider.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_BASE_PACKAGE);
		assertThat(candidates.size()).isEqualTo(6);
		assertThat(containsBeanClass(candidates, NamedComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, ServiceInvocationCounter.class)).isTrue();
		assertThat(containsBeanClass(candidates, BarComponent.class)).isTrue();
		assertThat(containsBeanClass(candidates, FooServiceImpl.class)).isFalse();
	}

	@Test
	void withNullEnvironment() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
		assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class)).isFalse();
	}

	@Test
	void withInactiveProfile() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		ConfigurableEnvironment env = new StandardEnvironment();
		env.setActiveProfiles("other");
		provider.setEnvironment(env);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
		assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class)).isFalse();
	}

	@Test
	void withActiveProfile() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		ConfigurableEnvironment env = new StandardEnvironment();
		env.setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
		provider.setEnvironment(env);
		Set<BeanDefinition> candidates = provider.findCandidateComponents(TEST_PROFILE_PACKAGE);
		assertThat(containsBeanClass(candidates, ProfileAnnotatedComponent.class)).isTrue();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_noProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
		ctx.close();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_validProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
		ctx.register(ProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isTrue();
		ctx.close();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_validMetaAnnotatedProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles(DevComponent.PROFILE_NAME);
		ctx.register(ProfileMetaAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME)).isTrue();
		ctx.close();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_invalidProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles("other");
		ctx.register(ProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
		ctx.close();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_invalidMetaAnnotatedProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setActiveProfiles("other");
		ctx.register(ProfileMetaAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(ProfileMetaAnnotatedComponent.BEAN_NAME)).isFalse();
		ctx.close();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_defaultProfile() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
		// no active profiles are set
		ctx.register(DefaultProfileAnnotatedComponent.class);
		ctx.refresh();
		assertThat(ctx.containsBean(DefaultProfileAnnotatedComponent.BEAN_NAME)).isTrue();
		ctx.close();
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_defaultAndDevProfile() {
		Class<?> beanClass = DefaultAndDevProfileAnnotatedComponent.class;
		String beanName = DefaultAndDevProfileAnnotatedComponent.BEAN_NAME;
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			// no active profiles are set
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName)).isTrue();
			ctx.close();
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("dev");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName)).isTrue();
			ctx.close();
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("other");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName)).isFalse();
			ctx.close();
		}
	}

	@Test
	void integrationWithAnnotationConfigApplicationContext_metaProfile() {
		Class<?> beanClass = MetaProfileAnnotatedComponent.class;
		String beanName = MetaProfileAnnotatedComponent.BEAN_NAME;
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			// no active profiles are set
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName)).isTrue();
			ctx.close();
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("dev");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName)).isTrue();
			ctx.close();
		}
		{
			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
			ctx.getEnvironment().setDefaultProfiles(TEST_DEFAULT_PROFILE_NAME);
			ctx.getEnvironment().setActiveProfiles("other");
			ctx.register(beanClass);
			ctx.refresh();
			assertThat(ctx.containsBean(beanName)).isFalse();
			ctx.close();
		}
	}

	@Test
	void componentScanningFindsComponentsAnnotatedWithAnnotationsContainingNestedAnnotations() {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
		Set<BeanDefinition> components = provider.findCandidateComponents(AnnotatedComponent.class.getPackage().getName());
		assertThat(components).hasSize(1);
		assertThat(components.iterator().next().getBeanClassName()).isEqualTo(AnnotatedComponent.class.getName());
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
		candidates.forEach(c ->
			assertThat(c).isInstanceOf(ScannedGenericBeanDefinition.class)
		);
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
	@interface DefaultProfile {
	}

	@Profile("dev")
	@Retention(RetentionPolicy.RUNTIME)
	@interface DevProfile {
	}

}
