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

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests changes introduced for SPR-8874, allowing beans of primitive types to be looked
 * up via getBean(Class), or to be injected using @Autowired or @Injected or @Resource.
 * Prior to these changes, an attempt to lookup or inject a bean of type boolean would
 * fail because all spring beans are Objects, regardless of initial type due to the way
 * that ObjectFactory works.
 *
 * Now these attempts to lookup or inject primitive types work, thanks to simple changes
 * in AbstractBeanFactory using ClassUtils#isAssignable methods instead of the built-in
 * Class#isAssignableFrom. The former takes into account primitives and their object
 * wrapper types, whereas the latter does not.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class PrimitiveBeanLookupAndAutowiringTests {

	@Test
	public void primitiveLookupByName() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		boolean b = ctx.getBean("b", boolean.class);
		assertThat(b).isEqualTo(true);
		int i = ctx.getBean("i", int.class);
		assertThat(i).isEqualTo(42);
	}

	@Test
	public void primitiveLookupByType() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		boolean b = ctx.getBean(boolean.class);
		assertThat(b).isEqualTo(true);
		int i = ctx.getBean(int.class);
		assertThat(i).isEqualTo(42);
	}

	@Test
	public void primitiveAutowiredInjection() {
		ApplicationContext ctx =
				new AnnotationConfigApplicationContext(Config.class, AutowiredComponent.class);
		assertThat(ctx.getBean(AutowiredComponent.class).b).isEqualTo(true);
		assertThat(ctx.getBean(AutowiredComponent.class).i).isEqualTo(42);
	}

	@Test
	public void primitiveResourceInjection() {
		ApplicationContext ctx =
				new AnnotationConfigApplicationContext(Config.class, ResourceComponent.class);
		assertThat(ctx.getBean(ResourceComponent.class).b).isEqualTo(true);
		assertThat(ctx.getBean(ResourceComponent.class).i).isEqualTo(42);
	}


	@Configuration
	static class Config {
		@Bean
		public boolean b() {
			return true;
		}

		@Bean
		public int i() {
			return 42;
		}
	}


	static class AutowiredComponent {
		@Autowired boolean b;
		@Autowired int i;
	}


	static class ResourceComponent {
		@Resource boolean b;
		@Autowired int i;
	}
}
