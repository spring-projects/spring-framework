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

package org.springframework.scheduling.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncCyclicDependenceTests {

	@Test
	public void asyncCyclicDependence() throws ExecutionException, InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(AsyncConfig.class, AsyncService.class, AopAspect.class);
		ctx.refresh();
		AsyncService asyncService = ctx.getBean(AsyncService.class);
		assertThat(asyncService instanceof Advised).isEqualTo(true);
		Future<String> asyncFuture = asyncService.async();
		assertThat(asyncFuture.get().equals(Thread.currentThread().getName())).isEqualTo(false);
		ctx.close();
	}

	@EnableAsync
	@EnableAspectJAutoProxy
	@Configuration
	static class AsyncConfig {
	}

	static class AsyncService {
		@Autowired
		private AsyncService asyncService;

		@Async
		public Future<String> async() {
			System.out.println(asyncService);
			// System.out.println("async => " + Thread.currentThread().getName());
			return new AsyncResult<>(Thread.currentThread().getName());
		}
	}

	@Aspect
	static class AopAspect {
		@Around("execution(* org.springframework.scheduling.annotation.AsyncCyclicDependenceTests.AsyncService.*(..))")
		public Object logAspect(ProceedingJoinPoint joinPoint) throws Throwable {
			Object[] args = joinPoint.getArgs();
			Object result = joinPoint.proceed(args);
			// System.out.println("result => " + result);
			return result;
		}
	}

}
