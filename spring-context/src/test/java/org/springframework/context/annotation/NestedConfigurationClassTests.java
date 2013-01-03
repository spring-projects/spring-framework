/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import test.beans.TestBean;

/**
 * Tests ensuring that nested static @Configuration classes are automatically detected
 * and registered without the need for explicit registration or @Import. See SPR-8186.
 *
 * @author Chris Beams
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

		ctx.getBean(L0Config.class);
		ctx.getBean("l0Bean");

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName(), is("override-l0"));
	}

	@Test
	public void twoLevelsDeepWithInheritance() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(S1Config.class);
		ctx.refresh();

		ctx.getBean(S1Config.class);
		ctx.getBean("l0Bean");

		ctx.getBean(L0Config.L1Config.class);
		ctx.getBean("l1Bean");

		ctx.getBean(L0Config.L1Config.L2Config.class);
		ctx.getBean("l2Bean");

		// ensure that override order is correct
		assertThat(ctx.getBean("overrideBean", TestBean.class).getName(), is("override-s1"));
	}


	@Configuration
	static class L0Config {
		@Bean
		public TestBean l0Bean() {
			return new TestBean("l0");
		}

		@Bean
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
			protected static class L2Config {
				@Bean
				public TestBean l2Bean() {
					return new TestBean("l2");
				}

				@Bean
				public TestBean overrideBean() {
					return new TestBean("override-l2");
				}
			}
		}
	}


	@Configuration
	static class S1Config extends L0Config {
		@Override
		@Bean
		public TestBean overrideBean() {
			return new TestBean("override-s1");
		}
	}

}