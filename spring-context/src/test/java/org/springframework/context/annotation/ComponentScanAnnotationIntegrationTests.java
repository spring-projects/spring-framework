/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;

import example.scannable.CustomComponent;
import example.scannable.CustomStereotype;
import example.scannable.DefaultNamedComponent;
import example.scannable.FooService;
import example.scannable.MessageBean;
import example.scannable.ScopedProxyTestBean;
import example.scannable_implicitbasepackage.ComponentScanAnnotatedConfigWithImplicitBasePackage;
import example.scannable_implicitbasepackage.ConfigurableComponent;
import example.scannable_scoped.CustomScopeAnnotationBean;
import example.scannable_scoped.MyScope;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.CustomAutowireConfigurer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.ComponentScanParserTests.KustomAnnotationAutowiredBean;
import org.springframework.context.annotation.componentscan.simple.ClassWithNestedComponents;
import org.springframework.context.annotation.componentscan.simple.SimpleComponent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.tests.context.SimpleMapScope;
import org.springframework.util.SerializationTestUtils;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

/**
 * Integration tests for processing ComponentScan-annotated Configuration classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
@SuppressWarnings("resource")
public class ComponentScanAnnotationIntegrationTests {

	@Test
	public void controlScan() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.scan(example.scannable._package.class.getPackage().getName());
		ctx.refresh();
		assertThat("control scan for example.scannable package failed to register FooServiceImpl bean",
				ctx.containsBean("fooServiceImpl"), is(true));
	}

	@Test
	public void viaContextRegistration() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanAnnotatedConfig.class);
		ctx.refresh();
		ctx.getBean(ComponentScanAnnotatedConfig.class);
		ctx.getBean(TestBean.class);
		assertThat("config class bean not found", ctx.containsBeanDefinition("componentScanAnnotatedConfig"), is(true));
		assertThat("@ComponentScan annotated @Configuration class registered directly against " +
				"AnnotationConfigApplicationContext did not trigger component scanning as expected",
				ctx.containsBean("fooServiceImpl"), is(true));
	}

	@Test
	public void viaContextRegistration_WithValueAttribute() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanAnnotatedConfig_WithValueAttribute.class);
		ctx.refresh();
		ctx.getBean(ComponentScanAnnotatedConfig_WithValueAttribute.class);
		ctx.getBean(TestBean.class);
		assertThat("config class bean not found", ctx.containsBeanDefinition("componentScanAnnotatedConfig_WithValueAttribute"), is(true));
		assertThat("@ComponentScan annotated @Configuration class registered directly against " +
				"AnnotationConfigApplicationContext did not trigger component scanning as expected",
				ctx.containsBean("fooServiceImpl"), is(true));
	}

	@Test
	public void viaContextRegistration_FromPackageOfConfigClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanAnnotatedConfigWithImplicitBasePackage.class);
		ctx.refresh();
		ctx.getBean(ComponentScanAnnotatedConfigWithImplicitBasePackage.class);
		assertThat("config class bean not found", ctx.containsBeanDefinition("componentScanAnnotatedConfigWithImplicitBasePackage"), is(true));
		assertThat("@ComponentScan annotated @Configuration class registered directly against " +
				"AnnotationConfigApplicationContext did not trigger component scanning as expected",
				ctx.containsBean("scannedComponent"), is(true));
		assertThat("@Bean method overrides scanned class", ctx.getBean(ConfigurableComponent.class).isFlag(), is(true));
	}

	@Test
	public void viaContextRegistration_WithComposedAnnotation() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComposedAnnotationConfig.class);
		ctx.refresh();
		ctx.getBean(ComposedAnnotationConfig.class);
		ctx.getBean(SimpleComponent.class);
		ctx.getBean(ClassWithNestedComponents.NestedComponent.class);
		ctx.getBean(ClassWithNestedComponents.OtherNestedComponent.class);
		assertThat("config class bean not found",
				ctx.containsBeanDefinition("componentScanAnnotationIntegrationTests.ComposedAnnotationConfig"), is(true));
		assertThat("@ComponentScan annotated @Configuration class registered directly against " +
						"AnnotationConfigApplicationContext did not trigger component scanning as expected",
				ctx.containsBean("simpleComponent"), is(true));
	}

	@Test
	public void viaBeanRegistration() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerBeanDefinition("componentScanAnnotatedConfig",
				genericBeanDefinition(ComponentScanAnnotatedConfig.class).getBeanDefinition());
		bf.registerBeanDefinition("configurationClassPostProcessor",
				genericBeanDefinition(ConfigurationClassPostProcessor.class).getBeanDefinition());
		GenericApplicationContext ctx = new GenericApplicationContext(bf);
		ctx.refresh();
		ctx.getBean(ComponentScanAnnotatedConfig.class);
		ctx.getBean(TestBean.class);
		assertThat("config class bean not found", ctx.containsBeanDefinition("componentScanAnnotatedConfig"), is(true));
		assertThat("@ComponentScan annotated @Configuration class registered " +
				"as bean definition did not trigger component scanning as expected",
				ctx.containsBean("fooServiceImpl"), is(true));
	}

	@Test
	public void withCustomBeanNameGenerator() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithBeanNameGenerator.class);
		ctx.refresh();
		assertThat(ctx.containsBean("custom_fooServiceImpl"), is(true));
		assertThat(ctx.containsBean("fooServiceImpl"), is(false));
	}

	@Test
	public void withScopeResolver() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithScopeResolver.class);
		// custom scope annotation makes the bean prototype scoped. subsequent calls
		// to getBean should return distinct instances.
		assertThat(ctx.getBean(CustomScopeAnnotationBean.class), not(sameInstance(ctx.getBean(CustomScopeAnnotationBean.class))));
		assertThat(ctx.containsBean("scannedComponent"), is(false));
	}

	@Test
	public void multiComponentScan() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MultiComponentScan.class);
		assertThat(ctx.getBean(CustomScopeAnnotationBean.class), not(sameInstance(ctx.getBean(CustomScopeAnnotationBean.class))));
		assertThat(ctx.containsBean("scannedComponent"), is(true));
	}

	@Test
	public void withCustomTypeFilter() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithCustomTypeFilter.class);
		assertFalse(ctx.getDefaultListableBeanFactory().containsSingleton("componentScanParserTests.KustomAnnotationAutowiredBean"));
		KustomAnnotationAutowiredBean testBean = ctx.getBean("componentScanParserTests.KustomAnnotationAutowiredBean", KustomAnnotationAutowiredBean.class);
		assertThat(testBean.getDependency(), notNullValue());
	}

	@Test
	public void withAwareTypeFilter() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ComponentScanWithAwareTypeFilter.class);
		assertTrue(ctx.getEnvironment().acceptsProfiles("the-filter-ran"));
	}

	@Test
	public void withScopedProxy() throws IOException, ClassNotFoundException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithScopedProxy.class);
		ctx.getBeanFactory().registerScope("myScope", new SimpleMapScope());
		ctx.refresh();
		// should cast to the interface
		FooService bean = (FooService) ctx.getBean("scopedProxyTestBean");
		// should be dynamic proxy
		assertThat(AopUtils.isJdkDynamicProxy(bean), is(true));
		// test serializability
		assertThat(bean.foo(1), equalTo("bar"));
		FooService deserialized = (FooService) SerializationTestUtils.serializeAndDeserialize(bean);
		assertThat(deserialized, notNullValue());
		assertThat(deserialized.foo(1), equalTo("bar"));
	}

	@Test
	public void withScopedProxyThroughRegex() throws IOException, ClassNotFoundException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithScopedProxyThroughRegex.class);
		ctx.getBeanFactory().registerScope("myScope", new SimpleMapScope());
		ctx.refresh();
		// should cast to the interface
		FooService bean = (FooService) ctx.getBean("scopedProxyTestBean");
		// should be dynamic proxy
		assertThat(AopUtils.isJdkDynamicProxy(bean), is(true));
	}

	@Test
	public void withScopedProxyThroughAspectJPattern() throws IOException, ClassNotFoundException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithScopedProxyThroughAspectJPattern.class);
		ctx.getBeanFactory().registerScope("myScope", new SimpleMapScope());
		ctx.refresh();
		// should cast to the interface
		FooService bean = (FooService) ctx.getBean("scopedProxyTestBean");
		// should be dynamic proxy
		assertThat(AopUtils.isJdkDynamicProxy(bean), is(true));
	}

	@Test
	public void withMultipleAnnotationIncludeFilters1() throws IOException, ClassNotFoundException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithMultipleAnnotationIncludeFilters1.class);
		ctx.refresh();
		ctx.getBean(DefaultNamedComponent.class); // @CustomStereotype-annotated
		ctx.getBean(MessageBean.class);           // @CustomComponent-annotated
	}

	@Test
	public void withMultipleAnnotationIncludeFilters2() throws IOException, ClassNotFoundException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithMultipleAnnotationIncludeFilters2.class);
		ctx.refresh();
		ctx.getBean(DefaultNamedComponent.class); // @CustomStereotype-annotated
		ctx.getBean(MessageBean.class);           // @CustomComponent-annotated
	}

	@Test
	public void withBasePackagesAndValueAlias() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ComponentScanWithBasePackagesAndValueAlias.class);
		ctx.refresh();
		assertThat(ctx.containsBean("fooServiceImpl"), is(true));
	}


	@Configuration
	@ComponentScan
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface ComposedConfiguration {

		String[] basePackages() default {};
	}

	@ComposedConfiguration(basePackages = "org.springframework.context.annotation.componentscan.simple")
	public static class ComposedAnnotationConfig {
	}

	public static class AwareTypeFilter implements TypeFilter, EnvironmentAware,
			ResourceLoaderAware, BeanClassLoaderAware, BeanFactoryAware {

		private BeanFactory beanFactory;
		private ClassLoader classLoader;
		private ResourceLoader resourceLoader;
		private Environment environment;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
			((ConfigurableEnvironment) this.environment).addActiveProfile("the-filter-ran");
			assertNotNull(this.beanFactory);
			assertNotNull(this.classLoader);
			assertNotNull(this.resourceLoader);
			assertNotNull(this.environment);
			return false;
		}

	}


}


@Configuration
@ComponentScan(basePackageClasses = example.scannable._package.class)
class ComponentScanAnnotatedConfig {

	@Bean
	public TestBean testBean() {
		return new TestBean();
	}
}

@Configuration
@ComponentScan("example.scannable")
class ComponentScanAnnotatedConfig_WithValueAttribute {

	@Bean
	public TestBean testBean() {
		return new TestBean();
	}
}

@Configuration
@ComponentScan
class ComponentScanWithNoPackagesConfig {
}

@Configuration
@ComponentScan(basePackages = "example.scannable", nameGenerator = MyBeanNameGenerator.class)
class ComponentScanWithBeanNameGenerator {
}

class MyBeanNameGenerator extends AnnotationBeanNameGenerator {

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return "custom_" + super.generateBeanName(definition, registry);
	}
}

@Configuration
@ComponentScan(basePackages = "example.scannable_scoped", scopeResolver = MyScopeMetadataResolver.class)
class ComponentScanWithScopeResolver {
}

@Configuration
@ComponentScan(basePackages = "example.scannable_scoped", scopeResolver = MyScopeMetadataResolver.class)
@ComponentScan(basePackages = "example.scannable_implicitbasepackage")
class MultiComponentScan {
}

class MyScopeMetadataResolver extends AnnotationScopeMetadataResolver {

	MyScopeMetadataResolver() {
		this.scopeAnnotationType = MyScope.class;
	}
}

@Configuration
@ComponentScan(
		basePackages = "org.springframework.context.annotation",
		useDefaultFilters = false,
		includeFilters = @Filter(type = FilterType.CUSTOM, classes = ComponentScanParserTests.CustomTypeFilter.class),
		// exclude this class from scanning since it's in the scanned package
		excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ComponentScanWithCustomTypeFilter.class),
		lazyInit = true)
class ComponentScanWithCustomTypeFilter {

	@Bean
	@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
	public static CustomAutowireConfigurer customAutowireConfigurer() {
		CustomAutowireConfigurer cac = new CustomAutowireConfigurer();
		cac.setCustomQualifierTypes(new HashSet() {{ add(ComponentScanParserTests.CustomAnnotation.class); }});
		return cac;
	}

	public ComponentScanParserTests.KustomAnnotationAutowiredBean testBean() {
		return new ComponentScanParserTests.KustomAnnotationAutowiredBean();
	}
}

@Configuration
@ComponentScan(
		basePackages = "org.springframework.context.annotation",
		useDefaultFilters = false,
		includeFilters = @Filter(type = FilterType.CUSTOM, classes = ComponentScanAnnotationIntegrationTests.AwareTypeFilter.class),
		lazyInit = true)
class ComponentScanWithAwareTypeFilter {}

@Configuration
@ComponentScan(basePackages = "example.scannable",
		scopedProxy = ScopedProxyMode.INTERFACES,
		useDefaultFilters = false,
		includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScopedProxyTestBean.class))
class ComponentScanWithScopedProxy {}

@Configuration
@ComponentScan(basePackages = "example.scannable",
		scopedProxy = ScopedProxyMode.INTERFACES,
		useDefaultFilters = false,
		includeFilters = @Filter(type=FilterType.REGEX, pattern = "((?:[a-z.]+))ScopedProxyTestBean"))
class ComponentScanWithScopedProxyThroughRegex {}

@Configuration
@ComponentScan(basePackages = "example.scannable",
		scopedProxy = ScopedProxyMode.INTERFACES,
		useDefaultFilters = false,
		includeFilters = @Filter(type=FilterType.ASPECTJ, pattern = "*..ScopedProxyTestBean"))
class ComponentScanWithScopedProxyThroughAspectJPattern {}

@Configuration
@ComponentScan(basePackages = "example.scannable",
		useDefaultFilters = false,
		includeFilters = {
			@Filter(CustomStereotype.class),
			@Filter(CustomComponent.class)
		}
	)
class ComponentScanWithMultipleAnnotationIncludeFilters1 {}

@Configuration
@ComponentScan(basePackages = "example.scannable",
		useDefaultFilters = false,
		includeFilters = @Filter({CustomStereotype.class, CustomComponent.class})
	)
class ComponentScanWithMultipleAnnotationIncludeFilters2 {}

@Configuration
@ComponentScan(
		value = "example.scannable",
		basePackages = "example.scannable",
		basePackageClasses = example.scannable._package.class)
class ComponentScanWithBasePackagesAndValueAlias {}


