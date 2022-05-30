/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.Closeable;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class DestroyMethodInferenceTests {

	@Test
	public void beanMethods() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
		WithExplicitDestroyMethod c0 = ctx.getBean(WithExplicitDestroyMethod.class);
		WithLocalCloseMethod c1 = ctx.getBean("c1", WithLocalCloseMethod.class);
		WithLocalCloseMethod c2 = ctx.getBean("c2", WithLocalCloseMethod.class);
		WithInheritedCloseMethod c3 = ctx.getBean("c3", WithInheritedCloseMethod.class);
		WithInheritedCloseMethod c4 = ctx.getBean("c4", WithInheritedCloseMethod.class);
		WithInheritedCloseMethod c5 = ctx.getBean("c5", WithInheritedCloseMethod.class);
		WithNoCloseMethod c6 = ctx.getBean("c6", WithNoCloseMethod.class);
		WithLocalShutdownMethod c7 = ctx.getBean("c7", WithLocalShutdownMethod.class);
		WithInheritedCloseMethod c8 = ctx.getBean("c8", WithInheritedCloseMethod.class);
		WithDisposableBean c9 = ctx.getBean("c9", WithDisposableBean.class);
		WithAutoCloseable c10 = ctx.getBean("c10", WithAutoCloseable.class);

		assertThat(c0.closed).as("c0").isFalse();
		assertThat(c1.closed).as("c1").isFalse();
		assertThat(c2.closed).as("c2").isFalse();
		assertThat(c3.closed).as("c3").isFalse();
		assertThat(c4.closed).as("c4").isFalse();
		assertThat(c5.closed).as("c5").isFalse();
		assertThat(c6.closed).as("c6").isFalse();
		assertThat(c7.closed).as("c7").isFalse();
		assertThat(c8.closed).as("c8").isFalse();
		assertThat(c9.closed).as("c9").isFalse();
		assertThat(c10.closed).as("c10").isFalse();

		ctx.close();
		assertThat(c0.closed).as("c0").isTrue();
		assertThat(c1.closed).as("c1").isTrue();
		assertThat(c2.closed).as("c2").isTrue();
		assertThat(c3.closed).as("c3").isTrue();
		assertThat(c4.closed).as("c4").isTrue();
		assertThat(c5.closed).as("c5").isTrue();
		assertThat(c6.closed).as("c6").isFalse();
		assertThat(c7.closed).as("c7").isTrue();
		assertThat(c8.closed).as("c8").isFalse();
		assertThat(c9.closed).as("c9").isTrue();
		assertThat(c10.closed).as("c10").isTrue();
	}

	@Test
	public void xml() {
		ConfigurableApplicationContext ctx = new GenericXmlApplicationContext(
				getClass(), "DestroyMethodInferenceTests-context.xml");
		WithLocalCloseMethod x1 = ctx.getBean("x1", WithLocalCloseMethod.class);
		WithLocalCloseMethod x2 = ctx.getBean("x2", WithLocalCloseMethod.class);
		WithLocalCloseMethod x3 = ctx.getBean("x3", WithLocalCloseMethod.class);
		WithNoCloseMethod x4 = ctx.getBean("x4", WithNoCloseMethod.class);
		WithInheritedCloseMethod x8 = ctx.getBean("x8", WithInheritedCloseMethod.class);
		WithDisposableBean x9 = ctx.getBean("x9", WithDisposableBean.class);
		WithAutoCloseable x10 = ctx.getBean("x10", WithAutoCloseable.class);

		assertThat(x1.closed).isFalse();
		assertThat(x2.closed).isFalse();
		assertThat(x3.closed).isFalse();
		assertThat(x4.closed).isFalse();
		assertThat(x8.closed).isFalse();
		assertThat(x9.closed).isFalse();
		assertThat(x10.closed).isFalse();

		ctx.close();
		assertThat(x1.closed).isFalse();
		assertThat(x2.closed).isTrue();
		assertThat(x3.closed).isTrue();
		assertThat(x4.closed).isFalse();
		assertThat(x8.closed).isFalse();
		assertThat(x9.closed).isTrue();
		assertThat(x10.closed).isTrue();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean(destroyMethod = "explicitClose")
		public WithExplicitDestroyMethod c0() {
			return new WithExplicitDestroyMethod();
		}

		@Bean
		public WithLocalCloseMethod c1() {
			return new WithLocalCloseMethod();
		}

		@Bean
		public Object c2() {
			return new WithLocalCloseMethod();
		}

		@Bean
		public WithInheritedCloseMethod c3() {
			return new WithInheritedCloseMethod();
		}

		@Bean
		public Closeable c4() {
			return new WithInheritedCloseMethod();
		}

		@Bean(destroyMethod = "other")
		public WithInheritedCloseMethod c5() {
			return new WithInheritedCloseMethod() {
				@Override
				public void close() {
					throw new IllegalStateException("close() should not be called");
				}
				@SuppressWarnings("unused")
				public void other() {
					this.closed = true;
				}
			};
		}

		@Bean
		public WithNoCloseMethod c6() {
			return new WithNoCloseMethod();
		}

		@Bean
		public WithLocalShutdownMethod c7() {
			return new WithLocalShutdownMethod();
		}

		@Bean(destroyMethod = "")
		public WithInheritedCloseMethod c8() {
			return new WithInheritedCloseMethod();
		}

		@Bean(destroyMethod = "")
		public WithDisposableBean c9() {
			return new WithDisposableBean();
		}

		@Bean
		public WithAutoCloseable c10() {
			return new WithAutoCloseable();
		}
	}


	static class WithExplicitDestroyMethod {

		boolean closed = false;

		public void explicitClose() {
			closed = true;
		}
	}


	static class WithLocalCloseMethod {

		boolean closed = false;

		public void close() {
			closed = true;
		}
	}


	static class WithInheritedCloseMethod implements Closeable {

		boolean closed = false;

		@Override
		public void close() {
			closed = true;
		}
	}


	static class WithNoCloseMethod {

		boolean closed = false;
	}


	static class WithLocalShutdownMethod {

		boolean closed = false;

		public void shutdown() {
			closed = true;
		}
	}


	static class WithDisposableBean implements DisposableBean {

		boolean closed = false;

		@Override
		public void destroy() {
			closed = true;
		}
	}


	static class WithAutoCloseable implements AutoCloseable {

		boolean closed = false;

		@Override
		public void close() {
			closed = true;
		}
	}

}
