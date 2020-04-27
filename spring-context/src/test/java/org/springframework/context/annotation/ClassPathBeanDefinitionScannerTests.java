/*
 * Copyright 2002-2020 the original author or authors.
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

import example.scannable.CustomComponent;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.NamedStubDao;
import example.scannable.StubFooDao;
import org.aspectj.lang.annotation.Aspect;
import org.junit.Test;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation2.NamedStubDao2;
import org.springframework.context.index.CandidateComponentsTestClassLoader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.stereotype.Component;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ClassPathBeanDefinitionScannerTests {

	private static final String BASE_PACKAGE = "example.scannable";


	@Test
	public void testSimpleScanWithDefaultFiltersAndPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean("thoreau"));
		assertTrue(context.containsBean(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
		context.refresh();

		FooServiceImpl fooService = context.getBean("fooServiceImpl", FooServiceImpl.class);
		assertTrue(context.getDefaultListableBeanFactory().containsSingleton("myNamedComponent"));
		assertEquals("bar", fooService.foo(123));
		assertEquals("bar", fooService.lookupFoo(123));
		assertTrue(context.isPrototype("thoreau"));
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndPrimaryLazyBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.scan(BASE_PACKAGE);
		scanner.scan("org.springframework.context.annotation5");
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean("otherFooDao"));
		context.refresh();

		assertFalse(context.getBeanFactory().containsSingleton("otherFooDao"));
		assertFalse(context.getBeanFactory().containsSingleton("fooServiceImpl"));
		FooServiceImpl fooService = context.getBean("fooServiceImpl", FooServiceImpl.class);
		assertTrue(context.getBeanFactory().containsSingleton("otherFooDao"));
		assertEquals("other", fooService.foo(123));
		assertEquals("other", fooService.lookupFoo(123));
	}

	@Test
	public void testDoubleScan() {
		GenericApplicationContext context = new GenericApplicationContext();

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);

		ClassPathBeanDefinitionScanner scanner2 = new ClassPathBeanDefinitionScanner(context) {
			@Override
			protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
				super.postProcessBeanDefinition(beanDefinition, beanName);
				beanDefinition.setAttribute("someDifference", "someValue");
			}
		};
		scanner2.scan(BASE_PACKAGE);

		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean("thoreau"));
	}

	@Test
	public void testWithIndex() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.setClassLoader(CandidateComponentsTestClassLoader.index(
				ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
				new ClassPathResource("spring.components", FooServiceImpl.class)));

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);

		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean("thoreau"));
	}

	@Test
	public void testDoubleScanWithIndex() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.setClassLoader(CandidateComponentsTestClassLoader.index(
				ClassPathScanningCandidateComponentProviderTests.class.getClassLoader(),
				new ClassPathResource("spring.components", FooServiceImpl.class)));

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);

		ClassPathBeanDefinitionScanner scanner2 = new ClassPathBeanDefinitionScanner(context) {
			@Override
			protected void postProcessBeanDefinition(AbstractBeanDefinition beanDefinition, String beanName) {
				super.postProcessBeanDefinition(beanDefinition, beanName);
				beanDefinition.setAttribute("someDifference", "someValue");
			}
		};
		scanner2.scan(BASE_PACKAGE);

		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean("thoreau"));
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndNoPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(7, beanCount);

		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndOverridingBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("stubFooDao", new RootBeanDefinition(TestBean.class));
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		// should not fail!
		scanner.scan(BASE_PACKAGE);
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndDefaultBeanNameClash() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		try {
			scanner.scan("org.springframework.context.annotation3");
			scanner.scan(BASE_PACKAGE);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// expected
			assertTrue(ex.getMessage().contains("stubFooDao"));
			assertTrue(ex.getMessage().contains(StubFooDao.class.getName()));
		}
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndOverriddenEqualNamedBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("myNamedDao", new RootBeanDefinition(NamedStubDao.class));
		int initialBeanCount = context.getBeanDefinitionCount();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		int scannedBeanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, scannedBeanCount);
		assertEquals(initialBeanCount + scannedBeanCount, context.getBeanDefinitionCount());
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndOverriddenCompatibleNamedBean() {
		GenericApplicationContext context = new GenericApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(NamedStubDao.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		context.registerBeanDefinition("myNamedDao", bd);
		int initialBeanCount = context.getBeanDefinitionCount();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		int scannedBeanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, scannedBeanCount);
		assertEquals(initialBeanCount + scannedBeanCount, context.getBeanDefinitionCount());
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndSameBeanTwice() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		// should not fail!
		scanner.scan(BASE_PACKAGE);
		scanner.scan(BASE_PACKAGE);
	}

	@Test
	public void testSimpleScanWithDefaultFiltersAndSpecifiedBeanNameClash() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		try {
			scanner.scan("org.springframework.context.annotation2");
			scanner.scan(BASE_PACKAGE);
			fail("Must have thrown IllegalStateException");
		}
		catch (IllegalStateException expected) {
			assertTrue(expected.getMessage().contains("myNamedDao"));
			assertTrue(expected.getMessage().contains(NamedStubDao.class.getName()));
			assertTrue(expected.getMessage().contains(NamedStubDao2.class.getName()));
		}
	}

	@Test
	public void testCustomIncludeFilterWithoutDefaultsButIncludingPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, beanCount);
		assertTrue(context.containsBean("messageBean"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
	}

	@Test
	public void testCustomIncludeFilterWithoutDefaultsAndNoPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, beanCount);
		assertTrue(context.containsBean("messageBean"));
		assertFalse(context.containsBean("serviceInvocationCounter"));
		assertFalse(context.containsBean("fooServiceImpl"));
		assertFalse(context.containsBean("stubFooDao"));
		assertFalse(context.containsBean("myNamedComponent"));
		assertFalse(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
	}

	@Test
	public void testCustomIncludeFilterAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addIncludeFilter(new AnnotationTypeFilter(CustomComponent.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(13, beanCount);
		assertTrue(context.containsBean("messageBean"));
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
	}

	@Test
	public void testCustomAnnotationExcludeFilterAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addExcludeFilter(new AnnotationTypeFilter(Aspect.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(11, beanCount);
		assertFalse(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
	}

	@Test
	public void testCustomAssignableTypeExcludeFilterAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(11, beanCount);
		assertFalse(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
	}

	@Test
	public void testCustomAssignableTypeExcludeFilterAndDefaultsWithoutPostProcessors() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.setIncludeAnnotationConfig(false);
		scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(6, beanCount);
		assertFalse(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertFalse(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertFalse(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
	}

	@Test
	public void testMultipleCustomExcludeFiltersAndDefaults() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context, true);
		scanner.addExcludeFilter(new AssignableTypeFilter(FooService.class));
		scanner.addExcludeFilter(new AnnotationTypeFilter(Aspect.class));
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(10, beanCount);
		assertFalse(context.containsBean("fooServiceImpl"));
		assertFalse(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
	}

	@Test
	public void testCustomBeanNameGenerator() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		int beanCount = scanner.scan(BASE_PACKAGE);

		assertEquals(12, beanCount);
		assertFalse(context.containsBean("fooServiceImpl"));
		assertTrue(context.containsBean("fooService"));
		assertTrue(context.containsBean("serviceInvocationCounter"));
		assertTrue(context.containsBean("stubFooDao"));
		assertTrue(context.containsBean("myNamedComponent"));
		assertTrue(context.containsBean("myNamedDao"));
		assertTrue(context.containsBean(AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		assertTrue(context.containsBean(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME));
	}

	@Test
	public void testMultipleBasePackagesWithDefaultsOnly() {
		GenericApplicationContext singlePackageContext = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner singlePackageScanner = new ClassPathBeanDefinitionScanner(singlePackageContext);
		GenericApplicationContext multiPackageContext = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner multiPackageScanner = new ClassPathBeanDefinitionScanner(multiPackageContext);
		int singlePackageBeanCount = singlePackageScanner.scan(BASE_PACKAGE);
		assertEquals(12, singlePackageBeanCount);
		multiPackageScanner.scan(BASE_PACKAGE, "org.springframework.dao.annotation");
		// assertTrue(multiPackageBeanCount > singlePackageBeanCount);
	}

	@Test
	public void testMultipleScanCalls() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		int initialBeanCount = context.getBeanDefinitionCount();
		int scannedBeanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, scannedBeanCount);
		assertEquals(scannedBeanCount, context.getBeanDefinitionCount() - initialBeanCount);
		int addedBeanCount = scanner.scan("org.springframework.aop.aspectj.annotation");
		assertEquals(initialBeanCount + scannedBeanCount + addedBeanCount, context.getBeanDefinitionCount());
	}

	@Test
	public void testBeanAutowiredWithAnnotationConfigEnabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("myBf", new RootBeanDefinition(StaticListableBeanFactory.class));
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(12, beanCount);
		context.refresh();

		FooServiceImpl fooService = context.getBean("fooService", FooServiceImpl.class);
		StaticListableBeanFactory myBf = (StaticListableBeanFactory) context.getBean("myBf");
		MessageSource ms = (MessageSource) context.getBean("messageSource");
		assertTrue(fooService.isInitCalled());
		assertEquals("bar", fooService.foo(123));
		assertEquals("bar", fooService.lookupFoo(123));
		assertSame(context.getDefaultListableBeanFactory(), fooService.beanFactory);
		assertEquals(2, fooService.listableBeanFactory.size());
		assertSame(context.getDefaultListableBeanFactory(), fooService.listableBeanFactory.get(0));
		assertSame(myBf, fooService.listableBeanFactory.get(1));
		assertSame(context, fooService.resourceLoader);
		assertSame(context, fooService.resourcePatternResolver);
		assertSame(context, fooService.eventPublisher);
		assertSame(ms, fooService.messageSource);
		assertSame(context, fooService.context);
		assertEquals(1, fooService.configurableContext.length);
		assertSame(context, fooService.configurableContext[0]);
		assertSame(context, fooService.genericContext);
	}

	@Test
	public void testBeanNotAutowiredWithAnnotationConfigDisabled() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(false);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		int beanCount = scanner.scan(BASE_PACKAGE);
		assertEquals(7, beanCount);
		context.refresh();

		try {
			context.getBean("fooService");
		}
		catch (BeanCreationException expected) {
			assertTrue(expected.contains(BeanInstantiationException.class));
			// @Lookup method not substituted
		}
	}

	@Test
	public void testAutowireCandidatePatternMatches() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(true);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		scanner.setAutowireCandidatePatterns("*FooDao");
		scanner.scan(BASE_PACKAGE);
		context.refresh();

		FooServiceImpl fooService = (FooServiceImpl) context.getBean("fooService");
		assertEquals("bar", fooService.foo(123));
		assertEquals("bar", fooService.lookupFoo(123));
	}

	@Test
	public void testAutowireCandidatePatternDoesNotMatch() {
		GenericApplicationContext context = new GenericApplicationContext();
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);
		scanner.setIncludeAnnotationConfig(true);
		scanner.setBeanNameGenerator(new TestBeanNameGenerator());
		scanner.setAutowireCandidatePatterns("*NoSuchDao");
		scanner.scan(BASE_PACKAGE);

		try {
			context.refresh();
			context.getBean("fooService");
			fail("BeanCreationException expected; fooDao should not have been an autowire-candidate");
		}
		catch (BeanCreationException expected) {
			assertTrue(expected.getMostSpecificCause() instanceof NoSuchBeanDefinitionException);
		}
	}


	private static class TestBeanNameGenerator extends AnnotationBeanNameGenerator {

		@Override
		public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
			String beanName = super.generateBeanName(definition, registry);
			return beanName.replace("Impl", "");
		}
	}


	@Component("toBeIgnored")
	public class NonStaticInnerClass {
	}

}
