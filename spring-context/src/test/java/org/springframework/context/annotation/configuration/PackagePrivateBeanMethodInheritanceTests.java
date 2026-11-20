/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces SPR-8756, which has been marked as "won't fix" for reasons
 * described in the issue. Also demonstrates the suggested workaround.
 *
 * @author Chris Beams
 */
class PackagePrivateBeanMethodInheritanceTests {

	@Test
	void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ReproConfig.class);
		ctx.refresh();
		Foo foo1 = ctx.getBean("foo1", Foo.class);
		Foo foo2 = ctx.getBean("foo2", Foo.class);
		ctx.getBean("packagePrivateBar", Bar.class); // <-- i.e. @Bean was registered
		assertThat(foo1.bar).isNotEqualTo(foo2.bar); // <-- i.e. @Bean *not* enhanced
		ctx.close();
	}

	@Test
	void workaround() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(WorkaroundConfig.class);
		ctx.refresh();
		Foo foo1 = ctx.getBean("foo1", Foo.class);
		Foo foo2 = ctx.getBean("foo2", Foo.class);
		ctx.getBean("protectedBar", Bar.class);   // <-- i.e. @Bean was registered
		assertThat(foo1.bar).isEqualTo(foo2.bar); // <-- i.e. @Bean *was* enhanced
		ctx.close();
	}


	public static class Foo {
		final Bar bar;
		public Foo(Bar bar) {
			this.bar = bar;
		}
	}

	@Configuration
	public static class ReproConfig extends org.springframework.context.annotation.configuration.a.BaseConfig {
		@Bean
		public Foo foo1() {
			return new Foo(reproBar());
		}

		@Bean
		public Foo foo2() {
			return new Foo(reproBar());
		}
	}

	@Configuration
	public static class WorkaroundConfig extends org.springframework.context.annotation.configuration.a.BaseConfig {
		@Bean
		public Foo foo1() {
			return new Foo(workaroundBar());
		}

		@Bean
		public Foo foo2() {
			return new Foo(workaroundBar());
		}
	}

}
