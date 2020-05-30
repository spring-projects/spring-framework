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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import example.scannable.FooDao;
import example.scannable.FooService;
import example.scannable.FooServiceImpl;
import example.scannable.ServiceInvocationCounter;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class EnableAspectJAutoProxyTests {

	@Test
	public void withJdkProxy() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithJdkProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class))).isTrue();
	}

	@Test
	public void withCglibProxy() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithCglibProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isCglibProxy(ctx.getBean(FooService.class))).isTrue();
	}

	@Test
	public void withExposedProxy() {
		ApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithExposedProxy.class);

		aspectIsApplied(ctx);
		assertThat(AopUtils.isJdkDynamicProxy(ctx.getBean(FooService.class))).isTrue();
	}

	private void aspectIsApplied(ApplicationContext ctx) {
		FooService fooService = ctx.getBean(FooService.class);
		ServiceInvocationCounter counter = ctx.getBean(ServiceInvocationCounter.class);

		assertThat(counter.getCount()).isEqualTo(0);

		assertThat(fooService.isInitCalled()).isTrue();
		assertThat(counter.getCount()).isEqualTo(1);

		String value = fooService.foo(1);
		assertThat(value).isEqualTo("bar");
		assertThat(counter.getCount()).isEqualTo(2);

		fooService.foo(1);
		assertThat(counter.getCount()).isEqualTo(3);
	}

	@Test
	public void withAnnotationOnArgumentAndJdkProxy() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				ConfigWithJdkProxy.class, SampleService.class, LoggingAspect.class);

		SampleService sampleService = ctx.getBean(SampleService.class);
		sampleService.execute(new SampleDto());
		sampleService.execute(new SampleInputBean());
		sampleService.execute((SampleDto) null);
		sampleService.execute((SampleInputBean) null);
	}

	@Test
	public void withAnnotationOnArgumentAndCglibProxy() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(
				ConfigWithCglibProxy.class, SampleService.class, LoggingAspect.class);

		SampleService sampleService = ctx.getBean(SampleService.class);
		sampleService.execute(new SampleDto());
		sampleService.execute(new SampleInputBean());
		sampleService.execute((SampleDto) null);
		sampleService.execute((SampleInputBean) null);
	}


	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy
	static class ConfigWithJdkProxy {
	}


	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class ConfigWithCglibProxy {
	}


	@ComponentScan("example.scannable")
	@EnableAspectJAutoProxy(exposeProxy = true)
	static class ConfigWithExposedProxy {

		@Bean
		public FooService fooServiceImpl(final ApplicationContext context) {
			return new FooServiceImpl() {
				@Override
				public String foo(int id) {
					assertThat(AopContext.currentProxy()).isNotNull();
					return super.foo(id);
				}
				@Override
				protected FooDao fooDao() {
					return context.getBean(FooDao.class);
				}
			};
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	public @interface Loggable {
	}


	@Loggable
	public static class SampleDto {
	}


	public static class SampleInputBean {
	}


	public static class SampleService {

		// Not matched method on {@link LoggingAspect}.
		public void execute(SampleInputBean inputBean) {
		}

		// Matched method on {@link LoggingAspect}
		public void execute(SampleDto dto) {
		}
	}


	@Aspect
	public static class LoggingAspect {

		@Before("@args(org.springframework.context.annotation.EnableAspectJAutoProxyTests.Loggable))")
		public void loggingBeginByAtArgs() {
		}
	}

}
