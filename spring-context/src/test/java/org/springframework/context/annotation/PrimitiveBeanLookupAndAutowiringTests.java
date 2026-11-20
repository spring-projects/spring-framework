/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests changes introduced for SPR-8874, allowing beans of primitive types to be looked
 * up via getBean(Class), or to be injected using @Autowired or @Injected or @Resource.
 * Prior to these changes, an attempt to look up or inject a bean of type boolean would
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
class PrimitiveBeanLookupAndAutowiringTests {

	@Test
	void primitiveLookupByName() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		assertThat(ctx.getBean("b", boolean.class)).isTrue();
		assertThat(ctx.getBean("i", int.class)).isEqualTo(42);
		ctx.close();
	}

	@Test
	void primitiveLookupByType() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		assertThat(ctx.getBean(boolean.class)).isTrue();
		assertThat(ctx.getBean(int.class)).isEqualTo(42);
		ctx.close();
	}

	@Test
	void primitiveAutowiredInjection() {
		ConfigurableApplicationContext ctx =
				new AnnotationConfigApplicationContext(Config.class, AutowiredComponent.class);
		assertThat(ctx.getBean(AutowiredComponent.class).b).isTrue();
		assertThat(ctx.getBean(AutowiredComponent.class).i).isEqualTo(42);
		ctx.close();
	}

	@Test
	void primitiveResourceInjection() {
		ConfigurableApplicationContext ctx =
				new AnnotationConfigApplicationContext(Config.class, ResourceComponent.class);
		assertThat(ctx.getBean(ResourceComponent.class).b).isTrue();
		assertThat(ctx.getBean(ResourceComponent.class).i).isEqualTo(42);
		ctx.close();
	}


	@Configuration
	static class Config {
		@Bean
		boolean b() {
			return true;
		}

		@Bean
		int i() {
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
