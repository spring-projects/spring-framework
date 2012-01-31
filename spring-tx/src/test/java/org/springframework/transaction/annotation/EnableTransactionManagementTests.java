/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.transaction.annotation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CallCountingTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionNamespaceHandlerTests.TransactionalTestBean;

/**
 * Tests demonstrating use of @EnableTransactionManagement @Configuration classes.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class EnableTransactionManagementTests {

	@Test
	public void transactionProxyIsCreated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableTxConfig.class, TxManagerConfig.class);
		ctx.refresh();
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		assertThat("testBean is not a proxy", AopUtils.isAopProxy(bean), is(true));
		Map<?,?> services = ctx.getBeansWithAnnotation(Service.class);
		assertThat("Stereotype annotation not visible", services.containsKey("testBean"), is(true));
	}

	@Test
	public void txManagerIsResolvedOnInvocationOfTransactionalMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableTxConfig.class, TxManagerConfig.class);
		ctx.refresh();
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();
	}

	@Test
	public void txManagerIsResolvedCorrectlyWhenMultipleManagersArePresent() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableTxConfig.class, MultiTxManagerConfig.class);
		ctx.refresh();
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();
	}

	/**
	 * A cheap test just to prove that in ASPECTJ mode, the AnnotationTransactionAspect does indeed
	 * get loaded -- or in this case, attempted to be loaded at which point the test fails.
	 */
	@Test
	public void proxyTypeAspectJCausesRegistrationOfAnnotationTransactionAspect() {
		try {
			new AnnotationConfigApplicationContext(EnableAspectJTxConfig.class, TxManagerConfig.class);
			fail("should have thrown CNFE when trying to load AnnotationTransactionAspect. " +
					"Do you actually have org.springframework.aspects on the classpath?");
		} catch (Exception ex) {
			System.out.println(ex);
			assertThat(ex.getMessage().endsWith("AspectJTransactionManagementConfiguration.class] cannot be opened because it does not exist"), is(true));
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class EnableTxConfig {
	}


	@Configuration
	@EnableTransactionManagement(mode=AdviceMode.ASPECTJ)
	static class EnableAspectJTxConfig {
	}


	@Configuration
	static class TxManagerConfig {

		@Bean
		public TransactionalTestBean testBean() {
			return new TransactionalTestBean();
		}

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}

	}


	@Configuration
	static class MultiTxManagerConfig extends TxManagerConfig implements TransactionManagementConfigurer {

		@Bean
		public PlatformTransactionManager txManager2() {
			return new CallCountingTransactionManager();
		}

		public PlatformTransactionManager annotationDrivenTransactionManager() {
			return txManager2();
		}
	}
}
