/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ensuring that nested static @Configuration classes are automatically detected
 * and registered without the need for explicit registration or @Import. See SPR-8186.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
class NestedConfigurationClassTests {

	@Test
	void oneLevelDeep() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(L0Config.L1Config.class);
		ctx.refresh();

		assertThat(ctx.containsBean("l0Bean")).isFalse();

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName()).isEqualTo("override-l1");
		ctx.close();
	}

	@Test
	void twoLevelsDeep() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(L0Config.class);
		ctx.refresh();

		assertThat(ctx.getBeanFactory().containsSingleton("nestedConfigurationClassTests.L0Config")).isFalse();
		ctx.getBean(L0Config.class);
		ctx.getBean("l0Bean");

		assertThat(ctx.getBeanFactory().containsSingleton(L0Config.L1Config.class.getName())).isTrue();
		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		assertThat(ctx.getBeanFactory().containsSingleton(L0Config.L1Config.L2Config.class.getName())).isFalse();
		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName()).isEqualTo("override-l0");
		ctx.close();
	}

	@Test
	void twoLevelsInLiteMode() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(L0ConfigLight.class);
		ctx.refresh();

		assertThat(ctx.getBeanFactory().containsSingleton("nestedConfigurationClassTests.L0ConfigLight")).isFalse();
		ctx.getBean(L0ConfigLight.class);
		ctx.getBean("l0Bean");

		assertThat(ctx.getBeanFactory().containsSingleton(L0ConfigLight.L1ConfigLight.class.getName())).isTrue();
		ctx.getBean(L0ConfigLight.L1ConfigLight.class);
		ctx.getBean("l1Bean");

		assertThat(ctx.getBeanFactory().containsSingleton(L0ConfigLight.L1ConfigLight.L2ConfigLight.class.getName())).isFalse();
		ctx.getBean(L0ConfigLight.L1ConfigLight.L2ConfigLight.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName()).isEqualTo("override-l0");
		ctx.close();
	}

	@Test
	void twoLevelsDeepWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(S1Config.class);
		ctx.refresh();

		S1Config config = ctx.getBean(S1Config.class);
		assertThat(config).isNotSameAs(ctx.getBean(S1Config.class));
		TestBean tb = ctx.getBean("l0Bean", TestBean.class);
		assertThat(tb).isSameAs(ctx.getBean("l0Bean", TestBean.class));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct and that it is a singleton
		TestBean ob = ctx.getBean("overrideBean", TestBean.class);
		assertThat(ob.getName()).isEqualTo("override-s1");
		assertThat(ob).isSameAs(ctx.getBean("overrideBean", TestBean.class));

		TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
		TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
		assertThat(pb1).isNotSameAs(pb2);
		assertThat(pb1.getFriends()).element(0).isNotSameAs(pb2.getFriends());
		ctx.close();
	}

	@Test
	void twoLevelsDeepWithInheritanceThroughImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(S1Importer.class);
		ctx.refresh();

		S1Config config = ctx.getBean(S1Config.class);
		assertThat(config).isNotSameAs(ctx.getBean(S1Config.class));
		TestBean tb = ctx.getBean("l0Bean", TestBean.class);
		assertThat(tb).isSameAs(ctx.getBean("l0Bean", TestBean.class));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct and that it is a singleton
		TestBean ob = ctx.getBean("overrideBean", TestBean.class);
		assertThat(ob.getName()).isEqualTo("override-s1");
		assertThat(ob).isSameAs(ctx.getBean("overrideBean", TestBean.class));

		TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
		TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
		assertThat(pb1).isNotSameAs(pb2);
		assertThat(pb1.getFriends()).element(0).isNotSameAs(pb2.getFriends());
		ctx.close();
	}

	@Test
	void twoLevelsDeepWithInheritanceAndScopedProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.setAllowBeanDefinitionOverriding(true);
		ctx.register(S1ImporterWithProxy.class);
		ctx.refresh();

		S1ConfigWithProxy config = ctx.getBean(S1ConfigWithProxy.class);
		assertThat(config).isSameAs(ctx.getBean(S1ConfigWithProxy.class));
		TestBean tb = ctx.getBean("l0Bean", TestBean.class);
		assertThat(tb).isSameAs(ctx.getBean("l0Bean", TestBean.class));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct and that it is a singleton
		TestBean ob = ctx.getBean("overrideBean", TestBean.class);
		assertThat(ob.getName()).isEqualTo("override-s1");
		assertThat(ob).isSameAs(ctx.getBean("overrideBean", TestBean.class));

		TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
		TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
		assertThat(pb1).isNotSameAs(pb2);
		assertThat(pb1.getFriends()).element(0).isNotSameAs(pb2.getFriends());
		ctx.close();
	}

	@Test
	void twoLevelsWithNoBeanMethods() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(L0ConfigEmpty.class);
		ctx.refresh();

		assertThat(ctx.getBeanFactory().containsSingleton("l0ConfigEmpty")).isFalse();
		Object l0i1 = ctx.getBean(L0ConfigEmpty.class);
		Object l0i2 = ctx.getBean(L0ConfigEmpty.class);
		assertThat(l0i1).isSameAs(l0i2);

		Object l1i1 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.class);
		Object l1i2 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.class);
		assertThat(l1i1).isNotSameAs(l1i2);

		Object l2i1 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.L2ConfigEmpty.class);
		Object l2i2 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.L2ConfigEmpty.class);
		assertThat(l2i1).isSameAs(l2i2);
		assertThat(l2i2.toString()).isNotEqualTo(l2i1.toString());
		ctx.close();
	}

	@Test
	void twoLevelsOnNonAnnotatedBaseClass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(L0ConfigConcrete.class);
		ctx.refresh();

		assertThat(ctx.getBeanFactory().containsSingleton("l0ConfigConcrete")).isFalse();
		Object l0i1 = ctx.getBean(L0ConfigConcrete.class);
		Object l0i2 = ctx.getBean(L0ConfigConcrete.class);
		assertThat(l0i1).isSameAs(l0i2);

		Object l1i1 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.class);
		Object l1i2 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.class);
		assertThat(l1i1).isNotSameAs(l1i2);

		Object l2i1 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.L2ConfigEmpty.class);
		Object l2i2 = ctx.getBean(L0ConfigConcrete.L1ConfigEmpty.L2ConfigEmpty.class);
		assertThat(l2i1).isSameAs(l2i2);
		assertThat(l2i2.toString()).isNotEqualTo(l2i1.toString());
		ctx.close();
	}


	@Configuration
	@Lazy
	static class L0Config {

		@Bean
		@Lazy
		public TestBean l0Bean() {
			return new TestBean("l0");
		}

		@Bean
		@Lazy
		public TestBean overrideBean() {
			return new TestBean("override-l0");
		}

		@Configuration
		static class L1Config {

			@Bean
			public TestBean l1Bean() {
				return new TestBean("l1");
			}

			@Bean
			public TestBean overrideBean() {
				return new TestBean("override-l1");
			}

			@Configuration
			@Lazy
			protected static class L2Config {

				@Bean
				@Lazy
				public TestBean l2Bean() {
					return new TestBean("l2");
				}

				@Bean
				@Lazy
				public TestBean overrideBean() {
					return new TestBean("override-l2");
				}
			}
		}
	}


	@Component
	@Lazy
	static class L0ConfigLight {

		@Bean
		@Lazy
		public TestBean l0Bean() {
			return new TestBean("l0");
		}

		@Bean
		@Lazy
		public TestBean overrideBean() {
			return new TestBean("override-l0");
		}

		@Component
		static class L1ConfigLight {

			@Bean
			public TestBean l1Bean() {
				return new TestBean("l1");
			}

			@Bean
			public TestBean overrideBean() {
				return new TestBean("override-l1");
			}

			@Component
			@Lazy
			protected static class L2ConfigLight {

				@Bean
				@Lazy
				public TestBean l2Bean() {
					return new TestBean("l2");
				}

				@Bean
				@Lazy
				public TestBean overrideBean() {
					return new TestBean("override-l2");
				}
			}
		}
	}


	@Configuration
	@Scope("prototype")
	static class S1Config extends L0Config {

		@Override
		@Bean
		public TestBean overrideBean() {
			return new TestBean("override-s1");
		}

		@Bean
		@Scope("prototype")
		public TestBean prototypeBean() {
			TestBean tb = new TestBean("override-s1");
			tb.getFriends().add(this);
			return tb;
		}
	}


	@Configuration
	@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class S1ConfigWithProxy extends L0Config {

		@Override
		@Bean
		public TestBean overrideBean() {
			return new TestBean("override-s1");
		}

		@Bean
		@Scope("prototype")
		public TestBean prototypeBean() {
			TestBean tb = new TestBean("override-s1");
			tb.getFriends().add(this);
			return tb;
		}
	}


	@Import(S1Config.class)
	static class S1Importer {
	}


	@Import(S1ConfigWithProxy.class)
	static class S1ImporterWithProxy {
	}


	@Component
	@Lazy
	static class L0ConfigEmpty {

		@Component
		@Scope("prototype")
		static class L1ConfigEmpty {

			@Component
			@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
			protected static class L2ConfigEmpty {
			}
		}
	}


	static class L0ConfigBase {

		@Component
		@Scope("prototype")
		static class L1ConfigEmpty {

			@Component
			@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
			protected static class L2ConfigEmpty {
			}
		}
	}


	@Component
	@Lazy
	static class L0ConfigConcrete extends L0ConfigBase {
	}

}
