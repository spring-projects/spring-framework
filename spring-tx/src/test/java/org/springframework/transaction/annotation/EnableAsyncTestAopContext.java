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

package org.springframework.transaction.annotation;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.tests.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

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
 *
 * * @author Li Zhi Long
 */
public class EnableAsyncTestAopContext {


	public static String mainThreadName;
	public static String async1ThreadName;
	public static String async2ThreadName;
	public static CountDownLatch countDownLatch;


	@Test
	public void asynExposeProxy() throws InterruptedException {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				AysncAppConfig.class);
		AsyncTransactionalTestBean bean = ctx.getBean(AsyncTransactionalTestBean.class);
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
		mainThreadName = Thread.currentThread().getName();

		countDownLatch = new CountDownLatch(2);
		bean.findAllFoos();
		countDownLatch.await();

		assertThat(mainThreadName.equals(async1ThreadName)).isFalse();
		assertThat(mainThreadName.equals(async2ThreadName)).isFalse();
		assertThat(async1ThreadName.equals(async2ThreadName)).isFalse();

		ctx.close();
	}


	@Service
	public static class AsyncTransactionalTestBean {


		@Transactional(readOnly = true)
		public Collection<?> findAllFoos() {
			System.out.println("saveQualifiedFoo " + Thread.currentThread().getName());
			((AsyncTransactionalTestBean) AopContext.currentProxy()).testAsync();
			return null;
		}


		@Async
		public void testAsync() {
			System.out.println("testAsync " + Thread.currentThread().getName());
			async1ThreadName = Thread.currentThread().getName();
			((AsyncTransactionalTestBean) AopContext.currentProxy()).testAsync1();
			countDownLatch.countDown();
		}


		@Async
		public void testAsync1() {
			System.out.println("testAsync1 " + Thread.currentThread().getName());
			async2ThreadName = Thread.currentThread().getName();
			countDownLatch.countDown();
		}


	}


	@EnableAsync
	@EnableAspectJAutoProxy(exposeProxy = true)
	@Configuration
	@EnableTransactionManagement
	static class AysncAppConfig {


		@Bean
		public AsyncTransactionalTestBean asyncTransactionalTestBean() {
			return new AsyncTransactionalTestBean();
		}


		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}


	}
}
