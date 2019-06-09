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

package org.springframework.context.annotation;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation6.ComponentForScanning;
import org.springframework.context.annotation6.ConfigForScanning;
import org.springframework.context.annotation6.Jsr330NamedForScanning;
import org.springframework.core.ResolvableType;
import org.springframework.util.ObjectUtils;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.util.StringUtils.uncapitalize;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class AnnotationConfigApplicationContextTests {

	@Test
	public void scanAndRefresh() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.scan("org.springframework.context.annotation6");
		context.refresh();

		context.getBean(uncapitalize(ConfigForScanning.class.getSimpleName()));
		context.getBean("testBean"); // contributed by ConfigForScanning
		context.getBean(uncapitalize(ComponentForScanning.class.getSimpleName()));
		context.getBean(uncapitalize(Jsr330NamedForScanning.class.getSimpleName()));
		Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
		assertThat(beans.size()).isEqualTo(1);
	}

	@Test
	public void registerAndRefresh() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class, NameConfig.class);
		context.refresh();

		context.getBean("testBean");
		context.getBean("name");
		Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
		assertThat(beans.size()).isEqualTo(2);
	}

	@Test
	public void getBeansWithAnnotation() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class, NameConfig.class, UntypedFactoryBean.class);
		context.refresh();

		context.getBean("testBean");
		context.getBean("name");
		Map<String, Object> beans = context.getBeansWithAnnotation(Configuration.class);
		assertThat(beans.size()).isEqualTo(2);
	}

	@Test
	public void getBeanByType() {
		ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		TestBean testBean = context.getBean(TestBean.class);
		assertThat(testBean).isNotNull();
		assertThat(testBean.name).isEqualTo("foo");
	}

	@Test
	public void getBeanByTypeRaisesNoSuchBeanDefinitionException() {
		ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		// attempt to retrieve a bean that does not exist
		Class<?> targetType = Pattern.class;
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				context.getBean(targetType))
			.withMessageContaining(format("No qualifying bean of type '%s'", targetType.getName()));
	}

	@Test
	public void getBeanByTypeAmbiguityRaisesException() {
		ApplicationContext context = new AnnotationConfigApplicationContext(TwoTestBeanConfig.class);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				context.getBean(TestBean.class))
			.withMessageContaining("No qualifying bean of type '" + TestBean.class.getName() + "'")
			.withMessageContaining("tb1")
			.withMessageContaining("tb2");
	}

	/**
	 * Tests that Configuration classes are registered according to convention
	 * @see org.springframework.beans.factory.support.DefaultBeanNameGenerator#generateBeanName
	 */
	@Test
	public void defaultConfigClassBeanNameIsGeneratedProperly() {
		ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

		// attempt to retrieve the instance by its generated bean name
		Config configObject = (Config) context.getBean("annotationConfigApplicationContextTests.Config");
		assertThat(configObject).isNotNull();
	}

	/**
	 * Tests that specifying @Configuration(value="foo") results in registering
	 * the configuration class with bean name 'foo'.
	 */
	@Test
	public void explicitConfigClassBeanNameIsRespected() {
		ApplicationContext context = new AnnotationConfigApplicationContext(ConfigWithCustomName.class);

		// attempt to retrieve the instance by its specified name
		ConfigWithCustomName configObject = (ConfigWithCustomName) context.getBean("customConfigBeanName");
		assertThat(configObject).isNotNull();
	}

	@Test
	public void autowiringIsEnabledByDefault() {
		ApplicationContext context = new AnnotationConfigApplicationContext(AutowiredConfig.class);
		assertThat(context.getBean(TestBean.class).name).isEqualTo("foo");
	}

	@Test
	public void nullReturningBeanPostProcessor() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(AutowiredConfig.class);
		context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				return (bean instanceof TestBean ? null : bean);
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				return bean;
			}
		});
		context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				bean.getClass().getName();
				return bean;
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				bean.getClass().getName();
				return bean;
			}
		});
		context.refresh();
	}

	@Test
	public void individualBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(BeanA.class, BeanB.class, BeanC.class);
		context.refresh();

		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualNamedBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("a", BeanA.class);
		context.registerBean("b", BeanB.class);
		context.registerBean("c", BeanC.class);
		context.refresh();

		assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b"));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualBeanWithSupplier() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("annotationConfigApplicationContextTests.BeanA")).isTrue();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

		assertThat(context.getDefaultListableBeanFactory().getDependentBeans("annotationConfigApplicationContextTests.BeanB")).isEqualTo(new String[] {"annotationConfigApplicationContextTests.BeanA"});
		assertThat(context.getDefaultListableBeanFactory().getDependentBeans("annotationConfigApplicationContextTests.BeanC")).isEqualTo(new String[] {"annotationConfigApplicationContextTests.BeanA"});
	}

	@Test
	public void individualBeanWithSupplierAndCustomizer() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("annotationConfigApplicationContextTests.BeanA")).isFalse();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualNamedBeanWithSupplier() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("a", BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("a")).isTrue();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualNamedBeanWithSupplierAndCustomizer() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("a", BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton("a")).isFalse();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualBeanWithNullReturningSupplier() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("a", BeanA.class, () -> null);
		context.registerBean("b", BeanB.class, BeanB::new);
		context.registerBean("c", BeanC.class, BeanC::new);
		context.refresh();

		assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanA.class), "a")).isTrue();
		assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanB.class), "b")).isTrue();
		assertThat(ObjectUtils.containsElement(context.getBeanNamesForType(BeanC.class), "c")).isTrue();
		assertThat(context.getBeansOfType(BeanA.class).isEmpty()).isTrue();
		assertThat(context.getBeansOfType(BeanB.class).values().iterator().next()).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBeansOfType(BeanC.class).values().iterator().next()).isSameAs(context.getBean(BeanC.class));
	}

	@Test
	public void individualBeanWithSpecifiedConstructorArguments() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanB b = new BeanB();
		BeanC c = new BeanC();
		context.registerBean(BeanA.class, b, c);
		context.refresh();

		assertThat(context.getBean(BeanA.class).b).isSameAs(b);
		assertThat(context.getBean(BeanA.class).c).isSameAs(c);
		assertThat((Object) b.applicationContext).isNull();
	}

	@Test
	public void individualNamedBeanWithSpecifiedConstructorArguments() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanB b = new BeanB();
		BeanC c = new BeanC();
		context.registerBean("a", BeanA.class, b, c);
		context.refresh();

		assertThat(context.getBean("a", BeanA.class).b).isSameAs(b);
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(c);
		assertThat((Object) b.applicationContext).isNull();
	}

	@Test
	public void individualBeanWithMixedConstructorArguments() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanC c = new BeanC();
		context.registerBean(BeanA.class, c);
		context.registerBean(BeanB.class);
		context.refresh();

		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(c);
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualNamedBeanWithMixedConstructorArguments() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanC c = new BeanC();
		context.registerBean("a", BeanA.class, c);
		context.registerBean("b", BeanB.class);
		context.refresh();

		assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b", BeanB.class));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(c);
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	public void individualBeanWithFactoryBeanSupplier() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.registerBean("fb", TypedFactoryBean.class, TypedFactoryBean::new, bd -> bd.setLazyInit(true));
		context.refresh();

		assertThat(context.getType("fb")).isEqualTo(String.class);
		assertThat(context.getType("&fb")).isEqualTo(TypedFactoryBean.class);
	}

	@Test
	public void individualBeanWithFactoryBeanSupplierAndTargetType() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition();
		bd.setInstanceSupplier(TypedFactoryBean::new);
		bd.setTargetType(ResolvableType.forClassWithGenerics(FactoryBean.class, String.class));
		bd.setLazyInit(true);
		context.registerBeanDefinition("fb", bd);
		context.refresh();

		assertThat(context.getType("fb")).isEqualTo(String.class);
		assertThat(context.getType("&fb")).isEqualTo(FactoryBean.class);
	}


	@Configuration
	static class Config {

		@Bean
		public TestBean testBean() {
			TestBean testBean = new TestBean();
			testBean.name = "foo";
			return testBean;
		}
	}

	@Configuration("customConfigBeanName")
	static class ConfigWithCustomName {

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

	@Configuration
	static class TwoTestBeanConfig {

		@Bean TestBean tb1() {
			return new TestBean();
		}

		@Bean TestBean tb2() {
			return new TestBean();
		}
	}

	@Configuration
	static class NameConfig {

		@Bean String name() { return "foo"; }
	}

	@Configuration
	@Import(NameConfig.class)
	static class AutowiredConfig {

		@Autowired String autowiredName;

		@Bean TestBean testBean() {
			TestBean testBean = new TestBean();
			testBean.name = autowiredName;
			return testBean;
		}
	}

	static class BeanA {

		BeanB b;
		BeanC c;

		@Autowired public BeanA(BeanB b, BeanC c) {
			this.b = b;
			this.c = c;
		}
	}

	static class BeanB {

		@Autowired ApplicationContext applicationContext;

		public BeanB() {
		}
	}

	static class BeanC {}

	static class TypedFactoryBean implements FactoryBean<String> {

		public TypedFactoryBean() {
			throw new IllegalStateException();
		}

		@Override
		public String getObject() {
			return "";
		}

		@Override
		public Class<?> getObjectType() {
			return String.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

	static class UntypedFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return null;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return false;
		}
	}
}

class TestBean {

	String name;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestBean other = (TestBean) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		return true;
	}

}
