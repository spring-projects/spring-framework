/*
 * Copyright 2002-2011 the original author or authors.
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

import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Reproduces SPR-8756, which has been marked as "won't fix" for reasons
 * described in the JIRA issue. Also demonstrates the suggested workaround.
 *
 * @author Chris Beams
 */
public class PackagePrivateBeanMethodInheritanceTests {

	@Test
	public void repro() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(ReproConfig.class);
		ctx.refresh();
		Foo foo1 = ctx.getBean("foo1", Foo.class);
		Foo foo2 = ctx.getBean("foo2", Foo.class);
		ctx.getBean("packagePrivateBar", Bar.class); // <-- i.e. @Bean was registered
		assertThat(foo1.bar, not(is(foo2.bar)));     // <-- i.e. @Bean *not* enhanced
	}

	@Test
	public void workaround() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(WorkaroundConfig.class);
		ctx.refresh();
		Foo foo1 = ctx.getBean("foo1", Foo.class);
		Foo foo2 = ctx.getBean("foo2", Foo.class);
		ctx.getBean("protectedBar", Bar.class); // <-- i.e. @Bean was registered
		assertThat(foo1.bar, is(foo2.bar));     // <-- i.e. @Bean *was* enhanced
	}

	public static class Foo {
		final Bar bar;
		public Foo(Bar bar) {
			this.bar = bar;
		}
	}

	public static class Bar {
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

