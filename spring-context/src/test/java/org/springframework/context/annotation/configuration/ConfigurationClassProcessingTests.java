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

package org.springframework.context.annotation.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Resource;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.NestedTestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Miscellaneous system tests covering {@link Bean} naming, aliases, scoping and
 * error handling within {@link Configuration} class definitions.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class ConfigurationClassProcessingTests {

	@Test
	public void customBeanNameIsRespectedWhenConfiguredViaNameAttribute() {
		customBeanNameIsRespected(ConfigWithBeanWithCustomName.class,
				() -> ConfigWithBeanWithCustomName.testBean, "customName");
	}

	@Test
	public void customBeanNameIsRespectedWhenConfiguredViaValueAttribute() {
		customBeanNameIsRespected(ConfigWithBeanWithCustomNameConfiguredViaValueAttribute.class,
				() -> ConfigWithBeanWithCustomNameConfiguredViaValueAttribute.testBean, "enigma");
	}

	private void customBeanNameIsRespected(Class<?> testClass, Supplier<TestBean> testBeanSupplier, String beanName) {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
		ac.registerBeanDefinition("config", new RootBeanDefinition(testClass));
		ac.refresh();

		assertThat(ac.getBean(beanName)).isSameAs(testBeanSupplier.get());

		// method name should not be registered
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				ac.getBean("methodName"));
	}

	@Test
	public void aliasesAreRespectedWhenConfiguredViaNameAttribute() {
		aliasesAreRespected(ConfigWithBeanWithAliases.class,
				() -> ConfigWithBeanWithAliases.testBean, "name1");
	}

	@Test
	public void aliasesAreRespectedWhenConfiguredViaValueAttribute() {
		aliasesAreRespected(ConfigWithBeanWithAliasesConfiguredViaValueAttribute.class,
				() -> ConfigWithBeanWithAliasesConfiguredViaValueAttribute.testBean, "enigma");
	}

	private void aliasesAreRespected(Class<?> testClass, Supplier<TestBean> testBeanSupplier, String beanName) {
		TestBean testBean = testBeanSupplier.get();
		BeanFactory factory = initBeanFactory(testClass);

		assertThat(factory.getBean(beanName)).isSameAs(testBean);
		Arrays.stream(factory.getAliases(beanName)).map(factory::getBean).forEach(alias -> assertThat(alias).isSameAs(testBean));

		// method name should not be registered
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				factory.getBean("methodName"));
	}

	@Test  // SPR-11830
	public void configWithBeanWithProviderImplementation() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
		ac.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithBeanWithProviderImplementation.class));
		ac.refresh();
		assertThat(ConfigWithBeanWithProviderImplementation.testBean).isSameAs(ac.getBean("customName"));
	}

	@Test  // SPR-11830
	public void configWithSetWithProviderImplementation() {
		GenericApplicationContext ac = new GenericApplicationContext();
		AnnotationConfigUtils.registerAnnotationConfigProcessors(ac);
		ac.registerBeanDefinition("config", new RootBeanDefinition(ConfigWithSetWithProviderImplementation.class));
		ac.refresh();
		assertThat(ConfigWithSetWithProviderImplementation.set).isSameAs(ac.getBean("customName"));
	}

	@Test
	public void testFinalBeanMethod() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
				initBeanFactory(ConfigWithFinalBean.class));
	}

	@Test
	public void simplestPossibleConfig() {
		BeanFactory factory = initBeanFactory(SimplestPossibleConfig.class);
		String stringBean = factory.getBean("stringBean", String.class);
		assertThat(stringBean).isEqualTo("foo");
	}

	@Test
	public void configWithObjectReturnType() {
		BeanFactory factory = initBeanFactory(ConfigWithNonSpecificReturnTypes.class);
		assertThat(factory.getType("stringBean")).isEqualTo(Object.class);
		assertThat(factory.isTypeMatch("stringBean", String.class)).isFalse();
		String stringBean = factory.getBean("stringBean", String.class);
		assertThat(stringBean).isEqualTo("foo");
	}

	@Test
	public void configWithFactoryBeanReturnType() {
		ListableBeanFactory factory = initBeanFactory(ConfigWithNonSpecificReturnTypes.class);
		assertThat(factory.getType("factoryBean")).isEqualTo(List.class);
		assertThat(factory.isTypeMatch("factoryBean", List.class)).isTrue();
		assertThat(factory.getType("&factoryBean")).isEqualTo(FactoryBean.class);
		assertThat(factory.isTypeMatch("&factoryBean", FactoryBean.class)).isTrue();
		assertThat(factory.isTypeMatch("&factoryBean", BeanClassLoaderAware.class)).isFalse();
		assertThat(factory.isTypeMatch("&factoryBean", ListFactoryBean.class)).isFalse();
		boolean condition = factory.getBean("factoryBean") instanceof List;
		assertThat(condition).isTrue();

		String[] beanNames = factory.getBeanNamesForType(FactoryBean.class);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = factory.getBeanNamesForType(BeanClassLoaderAware.class);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = factory.getBeanNamesForType(ListFactoryBean.class);
		assertThat(beanNames.length).isEqualTo(1);
		assertThat(beanNames[0]).isEqualTo("&factoryBean");

		beanNames = factory.getBeanNamesForType(List.class);
		assertThat(beanNames[0]).isEqualTo("factoryBean");
	}

	@Test
	public void configurationWithPrototypeScopedBeans() {
		BeanFactory factory = initBeanFactory(ConfigWithPrototypeBean.class);

		TestBean foo = factory.getBean("foo", TestBean.class);
		ITestBean bar = factory.getBean("bar", ITestBean.class);
		ITestBean baz = factory.getBean("baz", ITestBean.class);

		assertThat(bar).isSameAs(foo.getSpouse());
		assertThat(baz).isNotSameAs(bar.getSpouse());
	}

	@Test
	public void configurationWithNullReference() {
		BeanFactory factory = initBeanFactory(ConfigWithNullReference.class);

		TestBean foo = factory.getBean("foo", TestBean.class);
		assertThat(factory.getBean("bar").equals(null)).isTrue();
		assertThat(foo.getSpouse()).isNull();
	}

	@Test
	public void configurationWithAdaptivePrototypes() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithPrototypeBean.class, AdaptiveInjectionPoints.class);
		ctx.refresh();

		AdaptiveInjectionPoints adaptive = ctx.getBean(AdaptiveInjectionPoints.class);
		assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
		assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");

		adaptive = ctx.getBean(AdaptiveInjectionPoints.class);
		assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
		assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");
		ctx.close();
	}

	@Test
	public void configurationWithAdaptiveResourcePrototypes() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithPrototypeBean.class, AdaptiveResourceInjectionPoints.class);
		ctx.refresh();

		AdaptiveResourceInjectionPoints adaptive = ctx.getBean(AdaptiveResourceInjectionPoints.class);
		assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
		assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");

		adaptive = ctx.getBean(AdaptiveResourceInjectionPoints.class);
		assertThat(adaptive.adaptiveInjectionPoint1.getName()).isEqualTo("adaptiveInjectionPoint1");
		assertThat(adaptive.adaptiveInjectionPoint2.getName()).isEqualTo("setAdaptiveInjectionPoint2");
		ctx.close();
	}

	@Test
	public void configurationWithPostProcessor() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithPostProcessor.class);
		@SuppressWarnings("deprecation")
		RootBeanDefinition placeholderConfigurer = new RootBeanDefinition(
				org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.class);
		placeholderConfigurer.getPropertyValues().add("properties", "myProp=myValue");
		ctx.registerBeanDefinition("placeholderConfigurer", placeholderConfigurer);
		ctx.refresh();

		TestBean foo = ctx.getBean("foo", TestBean.class);
		ITestBean bar = ctx.getBean("bar", ITestBean.class);
		ITestBean baz = ctx.getBean("baz", ITestBean.class);

		assertThat(foo.getName()).isEqualTo("foo-processed-myValue");
		assertThat(bar.getName()).isEqualTo("bar-processed-myValue");
		assertThat(baz.getName()).isEqualTo("baz-processed-myValue");

		SpousyTestBean listener = ctx.getBean("listenerTestBean", SpousyTestBean.class);
		assertThat(listener.refreshed).isTrue();
		ctx.close();
	}

	@Test
	public void configurationWithFunctionalRegistration() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithFunctionalRegistration.class);
		ctx.refresh();

		assertThat(ctx.getBean(TestBean.class).getSpouse()).isSameAs(ctx.getBean("spouse"));
		assertThat(ctx.getBean(NestedTestBean.class).getCompany()).isEqualTo("functional");
	}

	@Test
	public void configurationWithApplicationListener() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ConfigWithApplicationListener.class);
		ctx.refresh();

		ConfigWithApplicationListener config = ctx.getBean(ConfigWithApplicationListener.class);
		assertThat(config.closed).isFalse();
		ctx.close();
		assertThat(config.closed).isTrue();
	}

	@Test
	public void configurationWithOverloadedBeanMismatch() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(OverloadedBeanMismatch.class));
		ctx.refresh();

		TestBean tb = ctx.getBean(TestBean.class);
		assertThat(tb.getLawyer()).isEqualTo(ctx.getBean(NestedTestBean.class));
	}

	@Test
	public void configurationWithOverloadedBeanMismatchWithAsm() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.registerBeanDefinition("config", new RootBeanDefinition(OverloadedBeanMismatch.class.getName()));
		ctx.refresh();

		TestBean tb = ctx.getBean(TestBean.class);
		assertThat(tb.getLawyer()).isEqualTo(ctx.getBean(NestedTestBean.class));
	}

	@Test  // gh-26019
	public void autowiringWithDynamicPrototypeBeanClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ConfigWithDynamicPrototype.class, PrototypeDependency.class);

		PrototypeInterface p1 = ctx.getBean(PrototypeInterface.class, 1);
		assertThat(p1).isInstanceOf(PrototypeOne.class);
		assertThat(((PrototypeOne) p1).prototypeDependency).isNotNull();

		PrototypeInterface p2 = ctx.getBean(PrototypeInterface.class, 2);
		assertThat(p2).isInstanceOf(PrototypeTwo.class);

		PrototypeInterface p3 = ctx.getBean(PrototypeInterface.class, 1);
		assertThat(p3).isInstanceOf(PrototypeOne.class);
		assertThat(((PrototypeOne) p3).prototypeDependency).isNotNull();
	}


	/**
	 * Creates a new {@link BeanFactory}, populates it with a {@link BeanDefinition}
	 * for each of the given {@link Configuration} {@code configClasses}, and then
	 * post-processes the factory using JavaConfig's {@link ConfigurationClassPostProcessor}.
	 * When complete, the factory is ready to service requests for any {@link Bean} methods
	 * declared by {@code configClasses}.
	 */
	private DefaultListableBeanFactory initBeanFactory(Class<?>... configClasses) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		for (Class<?> configClass : configClasses) {
			String configBeanName = configClass.getName();
			factory.registerBeanDefinition(configBeanName, new RootBeanDefinition(configClass));
		}
		ConfigurationClassPostProcessor ccpp = new ConfigurationClassPostProcessor();
		ccpp.postProcessBeanDefinitionRegistry(factory);
		ccpp.postProcessBeanFactory(factory);
		factory.freezeConfiguration();
		return factory;
	}


	@Configuration
	static class ConfigWithBeanWithCustomName {

		static TestBean testBean = new TestBean(ConfigWithBeanWithCustomName.class.getSimpleName());

		@Bean(name = "customName")
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithBeanWithCustomNameConfiguredViaValueAttribute {

		static TestBean testBean = new TestBean(ConfigWithBeanWithCustomNameConfiguredViaValueAttribute.class.getSimpleName());

		@Bean("enigma")
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithBeanWithAliases {

		static TestBean testBean = new TestBean(ConfigWithBeanWithAliases.class.getSimpleName());

		@Bean(name = {"name1", "alias1", "alias2", "alias3"})
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithBeanWithAliasesConfiguredViaValueAttribute {

		static TestBean testBean = new TestBean(ConfigWithBeanWithAliasesConfiguredViaValueAttribute.class.getSimpleName());

		@Bean({"enigma", "alias1", "alias2", "alias3"})
		public TestBean methodName() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithBeanWithProviderImplementation implements Provider<TestBean> {

		static TestBean testBean = new TestBean(ConfigWithBeanWithProviderImplementation.class.getSimpleName());

		@Override
		@Bean(name = "customName")
		public TestBean get() {
			return testBean;
		}
	}


	@Configuration
	static class ConfigWithSetWithProviderImplementation implements Provider<Set<String>> {

		static Set<String> set = Collections.singleton("value");

		@Override
		@Bean(name = "customName")
		public Set<String> get() {
			return set;
		}
	}


	@Configuration
	static class ConfigWithFinalBean {

		public final @Bean TestBean testBean() {
			return new TestBean();
		}
	}


	@Configuration
	static class SimplestPossibleConfig {

		public @Bean String stringBean() {
			return "foo";
		}
	}


	@Configuration
	static class ConfigWithNonSpecificReturnTypes {

		public @Bean Object stringBean() {
			return "foo";
		}

		public @Bean FactoryBean<?> factoryBean() {
			ListFactoryBean fb = new ListFactoryBean();
			fb.setSourceList(Arrays.asList("element1", "element2"));
			return fb;
		}
	}


	@Configuration
	static class ConfigWithPrototypeBean {

		public @Bean TestBean foo() {
			TestBean foo = new SpousyTestBean("foo");
			foo.setSpouse(bar());
			return foo;
		}

		public @Bean TestBean bar() {
			TestBean bar = new SpousyTestBean("bar");
			bar.setSpouse(baz());
			return bar;
		}

		@Bean @Scope("prototype")
		public TestBean baz() {
			return new TestBean("baz");
		}

		@Bean @Scope("prototype")
		public TestBean adaptive1(InjectionPoint ip) {
			return new TestBean(ip.getMember().getName());
		}

		@Bean @Scope("prototype")
		public TestBean adaptive2(DependencyDescriptor dd) {
			return new TestBean(dd.getMember().getName());
		}
	}


	@Configuration
	static class ConfigWithNullReference extends ConfigWithPrototypeBean {

		@Override
		public TestBean bar() {
			return null;
		}
	}


	@Scope("prototype")
	static class AdaptiveInjectionPoints {

		@Autowired @Qualifier("adaptive1")
		public TestBean adaptiveInjectionPoint1;

		public TestBean adaptiveInjectionPoint2;

		@Autowired @Qualifier("adaptive2")
		public void setAdaptiveInjectionPoint2(TestBean adaptiveInjectionPoint2) {
			this.adaptiveInjectionPoint2 = adaptiveInjectionPoint2;
		}
	}


	@Scope("prototype")
	static class AdaptiveResourceInjectionPoints {

		@Resource(name = "adaptive1")
		public TestBean adaptiveInjectionPoint1;

		public TestBean adaptiveInjectionPoint2;

		@Resource(name = "adaptive2")
		public void setAdaptiveInjectionPoint2(TestBean adaptiveInjectionPoint2) {
			this.adaptiveInjectionPoint2 = adaptiveInjectionPoint2;
		}
	}


	static class ConfigWithPostProcessor extends ConfigWithPrototypeBean {

		@Value("${myProp}")
		private String myProp;

		@Bean
		public POBPP beanPostProcessor() {
			return new POBPP() {

				String nameSuffix = "-processed-" + myProp;

				@SuppressWarnings("unused")
				public void setNameSuffix(String nameSuffix) {
					this.nameSuffix = nameSuffix;
				}

				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName) {
					if (bean instanceof ITestBean) {
						((ITestBean) bean).setName(((ITestBean) bean).getName() + nameSuffix);
					}
					return bean;
				}

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					return bean;
				}
			};
		}

		// @Bean
		public BeanFactoryPostProcessor beanFactoryPostProcessor() {
			return new BeanFactoryPostProcessor() {
				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
					BeanDefinition bd = beanFactory.getBeanDefinition("beanPostProcessor");
					bd.getPropertyValues().addPropertyValue("nameSuffix", "-processed-" + myProp);
				}
			};
		}

		@Bean
		public ITestBean listenerTestBean() {
			return new SpousyTestBean("listener");
		}
	}


	public interface POBPP extends BeanPostProcessor {
	}


	private static class SpousyTestBean extends TestBean implements ApplicationListener<ContextRefreshedEvent> {

		public boolean refreshed = false;

		public SpousyTestBean(String name) {
			super(name);
		}

		@Override
		public void setSpouse(ITestBean spouse) {
			super.setSpouse(spouse);
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			this.refreshed = true;
		}
	}


	@Configuration
	static class ConfigWithFunctionalRegistration {

		@Autowired
		void register(GenericApplicationContext ctx) {
			ctx.registerBean("spouse", TestBean.class,
					() -> new TestBean("functional"));
			Supplier<TestBean> testBeanSupplier = () -> new TestBean(ctx.getBean("spouse", TestBean.class));
			ctx.registerBean(TestBean.class,
					testBeanSupplier,
					bd -> bd.setPrimary(true));
		}

		@Bean
		public NestedTestBean nestedTestBean(TestBean testBean) {
			return new NestedTestBean(testBean.getSpouse().getName());
		}
	}


	@Configuration
	static class ConfigWithApplicationListener {

		boolean closed = false;

		@Bean
		public ApplicationListener<ContextClosedEvent> listener() {
			return (event -> this.closed = true);
		}
	}


	@Configuration
	public static class OverloadedBeanMismatch {

		@Bean(name = "other")
		public NestedTestBean foo() {
			return new NestedTestBean();
		}

		@Bean(name = "foo")
		public TestBean foo(@Qualifier("other") NestedTestBean other) {
			TestBean tb = new TestBean();
			tb.setLawyer(other);
			return tb;
		}
	}


	static class PrototypeDependency {
	}

	interface PrototypeInterface {
	}

	static class PrototypeOne extends AbstractPrototype {

		@Autowired
		PrototypeDependency prototypeDependency;

	}

	static class PrototypeTwo extends AbstractPrototype {

		// no autowired dependency here, in contrast to above
	}

	static class AbstractPrototype implements PrototypeInterface {
	}

	@Configuration
	static class ConfigWithDynamicPrototype {

		@Bean
		@Scope(value = "prototype")
		public PrototypeInterface getDemoBean( int i) {
			switch ( i) {
				case 1: return new PrototypeOne();
				case 2:
				default:
					return new PrototypeTwo();

			}
		}
	}

}
