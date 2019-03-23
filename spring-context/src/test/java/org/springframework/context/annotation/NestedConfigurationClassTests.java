/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

import org.springframework.stereotype.Component;
import org.springframework.tests.sample.beans.TestBean;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Tests ensuring that nested static @Configuration classes are automatically detected
 * and registered without the need for explicit registration or @Import. See SPR-8186.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class NestedConfigurationClassTests {

	@Test
	public void oneLevelDeep() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(L0Config.L1Config.class);
		ctx.refresh();

		assertFalse(ctx.containsBean("l0Bean"));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName(), is("override-l1"));
	}

	@Test
	public void twoLevelsDeep() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(L0Config.class);
		ctx.refresh();

		assertFalse(ctx.getBeanFactory().containsSingleton("nestedConfigurationClassTests.L0Config"));
		ctx.getBean(L0Config.class);
		ctx.getBean("l0Bean");

		assertTrue(ctx.getBeanFactory().containsSingleton(L0Config.L1Config.class.getName()));
		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		assertFalse(ctx.getBeanFactory().containsSingleton(L0Config.L1Config.L2Config.class.getName()));
		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName(), is("override-l0"));
	}

	@Test
	public void twoLevelsInLiteMode() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(L0ConfigLight.class);
		ctx.refresh();

		assertFalse(ctx.getBeanFactory().containsSingleton("nestedConfigurationClassTests.L0ConfigLight"));
		ctx.getBean(L0ConfigLight.class);
		ctx.getBean("l0Bean");

		assertTrue(ctx.getBeanFactory().containsSingleton(L0ConfigLight.L1ConfigLight.class.getName()));
		ctx.getBean(L0ConfigLight.L1ConfigLight.class);
		ctx.getBean("l1Bean");

		assertFalse(ctx.getBeanFactory().containsSingleton(L0ConfigLight.L1ConfigLight.L2ConfigLight.class.getName()));
		ctx.getBean(L0ConfigLight.L1ConfigLight.L2ConfigLight.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName(), is("override-l0"));
	}

	@Test
	public void twoLevelsDeepWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(S1Config.class);
		ctx.refresh();

		S1Config config = ctx.getBean(S1Config.class);
		assertTrue(config != ctx.getBean(S1Config.class));
		TestBean tb = ctx.getBean("l0Bean", TestBean.class);
		assertTrue(tb == ctx.getBean("l0Bean", TestBean.class));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct and that it is a singleton
		TestBean ob = ctx.getBean("overrideBean", TestBean.class);
		assertThat(ob.getName(), is("override-s1"));
		assertTrue(ob == ctx.getBean("overrideBean", TestBean.class));

		TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
		TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
		assertTrue(pb1 != pb2);
		assertTrue(pb1.getFriends().iterator().next() != pb2.getFriends().iterator().next());
	}

	@Test
	public void twoLevelsDeepWithInheritanceThroughImport() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(S1Importer.class);
		ctx.refresh();

		S1Config config = ctx.getBean(S1Config.class);
		assertTrue(config != ctx.getBean(S1Config.class));
		TestBean tb = ctx.getBean("l0Bean", TestBean.class);
		assertTrue(tb == ctx.getBean("l0Bean", TestBean.class));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct and that it is a singleton
		TestBean ob = ctx.getBean("overrideBean", TestBean.class);
		assertThat(ob.getName(), is("override-s1"));
		assertTrue(ob == ctx.getBean("overrideBean", TestBean.class));

		TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
		TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
		assertTrue(pb1 != pb2);
		assertTrue(pb1.getFriends().iterator().next() != pb2.getFriends().iterator().next());
	}

	@Test
	public void twoLevelsDeepWithInheritanceAndScopedProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(S1ImporterWithProxy.class);
		ctx.refresh();

		S1ConfigWithProxy config = ctx.getBean(S1ConfigWithProxy.class);
		assertTrue(config == ctx.getBean(S1ConfigWithProxy.class));
		TestBean tb = ctx.getBean("l0Bean", TestBean.class);
		assertTrue(tb == ctx.getBean("l0Bean", TestBean.class));

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct and that it is a singleton
		TestBean ob = ctx.getBean("overrideBean", TestBean.class);
		assertThat(ob.getName(), is("override-s1"));
		assertTrue(ob == ctx.getBean("overrideBean", TestBean.class));

		TestBean pb1 = ctx.getBean("prototypeBean", TestBean.class);
		TestBean pb2 = ctx.getBean("prototypeBean", TestBean.class);
		assertTrue(pb1 != pb2);
		assertTrue(pb1.getFriends().iterator().next() != pb2.getFriends().iterator().next());
	}

	@Test
	public void twoLevelsWithNoBeanMethods() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(L0ConfigEmpty.class);
		ctx.refresh();

		assertFalse(ctx.getBeanFactory().containsSingleton("l0ConfigEmpty"));
		Object l0i1 = ctx.getBean(L0ConfigEmpty.class);
		Object l0i2 = ctx.getBean(L0ConfigEmpty.class);
		assertTrue(l0i1 == l0i2);

		Object l1i1 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.class);
		Object l1i2 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.class);
		assertTrue(l1i1 != l1i2);

		Object l2i1 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.L2ConfigEmpty.class);
		Object l2i2 = ctx.getBean(L0ConfigEmpty.L1ConfigEmpty.L2ConfigEmpty.class);
		assertTrue(l2i1 == l2i2);
		assertNotEquals(l2i1.toString(), l2i2.toString());
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

}
