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

package org.springframework.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.metrics.jfr.FlightRecorderApplicationStartup;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class GenericApplicationContextTests {

	@Test
	void getBeanForClass() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		ac.refresh();

		assertThat(ac.getBean("testBean")).isEqualTo("");
		assertThat(ac.getBean(String.class)).isSameAs(ac.getBean("testBean"));
		assertThat(ac.getBean(CharSequence.class)).isSameAs(ac.getBean("testBean"));

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() ->
				ac.getBean(Object.class));
	}

	@Test
	void withSingletonSupplier() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class, ac::toString));
		ac.refresh();

		assertThat(ac.getBean("testBean")).isSameAs(ac.getBean("testBean"));
		assertThat(ac.getBean(String.class)).isSameAs(ac.getBean("testBean"));
		assertThat(ac.getBean(CharSequence.class)).isSameAs(ac.getBean("testBean"));
		assertThat(ac.getBean("testBean")).isEqualTo(ac.toString());
	}

	@Test
	void withScopedSupplier() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean",
				new RootBeanDefinition(String.class, BeanDefinition.SCOPE_PROTOTYPE, ac::toString));
		ac.refresh();

		assertThat(ac.getBean("testBean")).isNotSameAs(ac.getBean("testBean"));
		assertThat(ac.getBean(String.class)).isEqualTo(ac.getBean("testBean"));
		assertThat(ac.getBean(CharSequence.class)).isEqualTo(ac.getBean("testBean"));
		assertThat(ac.getBean("testBean")).isEqualTo(ac.toString());
	}

	@Test
	void accessAfterClosing() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.registerBeanDefinition("testBean", new RootBeanDefinition(String.class));
		ac.refresh();

		assertThat(ac.getBean(String.class)).isSameAs(ac.getBean("testBean"));
		assertThat(ac.getAutowireCapableBeanFactory().getBean(String.class)).isSameAs(ac.getAutowireCapableBeanFactory().getBean("testBean"));

		ac.close();

		assertThatIllegalStateException().isThrownBy(() ->
				ac.getBean(String.class));

		assertThatIllegalStateException().isThrownBy(() -> {
				ac.getAutowireCapableBeanFactory().getBean("testBean");
				ac.getAutowireCapableBeanFactory().getBean(String.class);
		});
	}

	@Test
	void individualBeans() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(BeanA.class);
		context.registerBean(BeanB.class);
		context.registerBean(BeanC.class);
		context.refresh();

		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualNamedBeans() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("a", BeanA.class);
		context.registerBean("b", BeanB.class);
		context.registerBean("c", BeanC.class);
		context.refresh();

		assertThat(context.getBean("a", BeanA.class).b).isSameAs(context.getBean("b"));
		assertThat(context.getBean("a", BeanA.class).c).isSameAs(context.getBean("c"));
		assertThat(context.getBean("b", BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualBeanWithSupplier() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isTrue();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);

		assertThat(context.getDefaultListableBeanFactory().getDependentBeans(BeanB.class.getName())).isEqualTo(new String[] {BeanA.class.getName()});
		assertThat(context.getDefaultListableBeanFactory().getDependentBeans(BeanC.class.getName())).isEqualTo(new String[] {BeanA.class.getName()});
	}

	@Test
	void individualBeanWithSupplierAndCustomizer() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(BeanA.class,
				() -> new BeanA(context.getBean(BeanB.class), context.getBean(BeanC.class)),
				bd -> bd.setLazyInit(true));
		context.registerBean(BeanB.class, BeanB::new);
		context.registerBean(BeanC.class, BeanC::new);
		context.refresh();

		assertThat(context.getBeanFactory().containsSingleton(BeanA.class.getName())).isFalse();
		assertThat(context.getBean(BeanA.class).b).isSameAs(context.getBean(BeanB.class));
		assertThat(context.getBean(BeanA.class).c).isSameAs(context.getBean(BeanC.class));
		assertThat(context.getBean(BeanB.class).applicationContext).isSameAs(context);
	}

	@Test
	void individualNamedBeanWithSupplier() {
		GenericApplicationContext context = new GenericApplicationContext();
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
	void individualNamedBeanWithSupplierAndCustomizer() {
		GenericApplicationContext context = new GenericApplicationContext();
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
	void individualBeanWithNullReturningSupplier() {
		GenericApplicationContext context = new GenericApplicationContext();
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
	void configureApplicationStartupOnBeanFactory() {
		FlightRecorderApplicationStartup applicationStartup = new FlightRecorderApplicationStartup();
		GenericApplicationContext context = new GenericApplicationContext();
		context.setApplicationStartup(applicationStartup);
		assertThat(context.getBeanFactory().getApplicationStartup()).isEqualTo(applicationStartup);
	}


	static class BeanA {

		BeanB b;
		BeanC c;

		public BeanA(BeanB b, BeanC c) {
			this.b = b;
			this.c = c;
		}
	}

	static class BeanB implements ApplicationContextAware  {

		ApplicationContext applicationContext;

		public BeanB() {
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}
	}

	static class BeanC {}

}
