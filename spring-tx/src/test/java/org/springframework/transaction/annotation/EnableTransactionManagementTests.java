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

package org.springframework.transaction.annotation;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.config.TransactionManagementConfigUtils;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests demonstrating use of @EnableTransactionManagement @Configuration classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 */
public class EnableTransactionManagementTests {

	@Test
	public void transactionProxyIsCreated() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				EnableTxConfig.class, TxManagerConfig.class);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
		Map<?,?> services = ctx.getBeansWithAnnotation(Service.class);
		assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
		ctx.close();
	}

	@Test
	public void transactionProxyIsCreatedWithEnableOnSuperclass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				InheritedEnableTxConfig.class, TxManagerConfig.class);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
		Map<?,?> services = ctx.getBeansWithAnnotation(Service.class);
		assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
		ctx.close();
	}

	@Test
	public void transactionProxyIsCreatedWithEnableOnExcludedSuperclass() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				ParentEnableTxConfig.class, ChildEnableTxConfig.class, TxManagerConfig.class);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
		Map<?,?> services = ctx.getBeansWithAnnotation(Service.class);
		assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
		ctx.close();
	}

	@Test
	public void txManagerIsResolvedOnInvocationOfTransactionalMethod() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				EnableTxConfig.class, TxManagerConfig.class);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		CallCountingTransactionManager txManager = ctx.getBean("txManager", CallCountingTransactionManager.class);

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();
		assertThat(txManager.begun).isEqualTo(1);
		assertThat(txManager.commits).isEqualTo(1);
		assertThat(txManager.rollbacks).isEqualTo(0);
		assertThat(txManager.lastDefinition.isReadOnly()).isTrue();
		assertThat(txManager.lastDefinition.getTimeout()).isEqualTo(5);
		assertThat(((TransactionAttribute) txManager.lastDefinition).getLabels()).contains("LABEL");

		ctx.close();
	}

	@Test
	public void txManagerIsResolvedCorrectlyWhenMultipleManagersArePresent() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				EnableTxConfig.class, MultiTxManagerConfig.class);
		assertThat(ctx.getBeansOfType(TransactionManager.class)).hasSize(2);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		CallCountingTransactionManager txManager = ctx.getBean("txManager", CallCountingTransactionManager.class);
		CallCountingTransactionManager txManager2 = ctx.getBean("txManager2", CallCountingTransactionManager.class);

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();
		assertThat(txManager.begun).isEqualTo(0);
		assertThat(txManager.commits).isEqualTo(0);
		assertThat(txManager.rollbacks).isEqualTo(0);
		assertThat(txManager2.begun).isEqualTo(1);
		assertThat(txManager2.commits).isEqualTo(1);
		assertThat(txManager2.rollbacks).isEqualTo(0);

		ctx.close();
	}

	@Test
	public void txManagerIsResolvedCorrectlyWhenMultipleManagersArePresentAndOneIsPrimary() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				EnableTxConfig.class, PrimaryMultiTxManagerConfig.class);
		assertThat(ctx.getBeansOfType(TransactionManager.class)).hasSize(2);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		CallCountingTransactionManager primary = ctx.getBean("primary", CallCountingTransactionManager.class);
		CallCountingTransactionManager txManager2 = ctx.getBean("txManager2", CallCountingTransactionManager.class);

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();

		assertThat(primary.begun).isEqualTo(1);
		assertThat(primary.commits).isEqualTo(1);
		assertThat(primary.rollbacks).isEqualTo(0);
		assertThat(txManager2.begun).isEqualTo(0);
		assertThat(txManager2.commits).isEqualTo(0);
		assertThat(txManager2.rollbacks).isEqualTo(0);

		ctx.close();
	}

	@Test
	public void txManagerIsResolvedCorrectlyWithTxMgmtConfigurerAndPrimaryPresent() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				EnableTxConfig.class, PrimaryTxManagerAndTxMgmtConfigurerConfig.class);
		assertThat(ctx.getBeansOfType(TransactionManager.class)).hasSize(2);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		CallCountingTransactionManager primary = ctx.getBean("primary", CallCountingTransactionManager.class);
		CallCountingTransactionManager annotationDriven = ctx.getBean("annotationDrivenTransactionManager", CallCountingTransactionManager.class);

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();

		assertThat(primary.begun).isEqualTo(0);
		assertThat(primary.commits).isEqualTo(0);
		assertThat(primary.rollbacks).isEqualTo(0);
		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.commits).isEqualTo(1);
		assertThat(annotationDriven.rollbacks).isEqualTo(0);

		ctx.close();
	}

	@Test
	public void txManagerIsResolvedCorrectlyWithSingleTxManagerBeanAndTxMgmtConfigurer() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
				EnableTxConfig.class, SingleTxManagerBeanAndTxMgmtConfigurerConfig.class);
		assertThat(ctx.getBeansOfType(TransactionManager.class)).hasSize(1);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
		SingleTxManagerBeanAndTxMgmtConfigurerConfig config = ctx.getBean(SingleTxManagerBeanAndTxMgmtConfigurerConfig.class);
		CallCountingTransactionManager annotationDriven = config.annotationDriven;

		// invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
		bean.findAllFoos();

		assertThat(txManager.begun).isEqualTo(0);
		assertThat(txManager.commits).isEqualTo(0);
		assertThat(txManager.rollbacks).isEqualTo(0);
		assertThat(annotationDriven.begun).isEqualTo(1);
		assertThat(annotationDriven.commits).isEqualTo(1);
		assertThat(annotationDriven.rollbacks).isEqualTo(0);

		ctx.close();
	}

	/**
	 * A cheap test just to prove that in ASPECTJ mode, the AnnotationTransactionAspect does indeed
	 * get loaded -- or in this case, attempted to be loaded at which point the test fails.
	 */
	@Test
	@SuppressWarnings("resource")
	public void proxyTypeAspectJCausesRegistrationOfAnnotationTransactionAspect() {
		// should throw CNFE when trying to load AnnotationTransactionAspect.
		// Do you actually have org.springframework.aspects on the classpath?
		assertThatExceptionOfType(Exception.class).isThrownBy(() ->
				new AnnotationConfigApplicationContext(EnableAspectjTxConfig.class, TxManagerConfig.class))
			.withMessageContaining("AspectJJtaTransactionManagementConfiguration");
	}

	@Test
	public void transactionalEventListenerRegisteredProperly() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(EnableTxConfig.class);
		assertThat(ctx.containsBean(TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
		assertThat(ctx.getBeansOfType(TransactionalEventListenerFactory.class).size()).isEqualTo(1);
		ctx.close();
	}

	@Test
	public void spr11915TransactionManagerAsManualSingleton() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Spr11915Config.class);
		TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
		CallCountingTransactionManager txManager = ctx.getBean("qualifiedTransactionManager", CallCountingTransactionManager.class);

		bean.saveQualifiedFoo();
		assertThat(txManager.begun).isEqualTo(1);
		assertThat(txManager.commits).isEqualTo(1);
		assertThat(txManager.rollbacks).isEqualTo(0);

		bean.saveQualifiedFooWithAttributeAlias();
		assertThat(txManager.begun).isEqualTo(2);
		assertThat(txManager.commits).isEqualTo(2);
		assertThat(txManager.rollbacks).isEqualTo(0);

		ctx.close();
	}

	@Test
	public void spr14322FindsOnInterfaceWithInterfaceProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Spr14322ConfigA.class);
		TransactionalTestInterface bean = ctx.getBean(TransactionalTestInterface.class);
		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);

		bean.saveFoo();
		bean.saveBar();
		assertThat(txManager.begun).isEqualTo(2);
		assertThat(txManager.commits).isEqualTo(2);
		assertThat(txManager.rollbacks).isEqualTo(0);

		ctx.close();
	}

	@Test
	public void spr14322FindsOnInterfaceWithCglibProxy() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Spr14322ConfigB.class);
		TransactionalTestInterface bean = ctx.getBean(TransactionalTestInterface.class);
		CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);

		bean.saveFoo();
		bean.saveBar();
		assertThat(txManager.begun).isEqualTo(2);
		assertThat(txManager.commits).isEqualTo(2);
		assertThat(txManager.rollbacks).isEqualTo(0);

		ctx.close();
	}


	@Service
	public static class TransactionalTestBean {

		@Transactional(label = "${myLabel}", timeoutString = "${myTimeout}", readOnly = true)
		public Collection<?> findAllFoos() {
			return null;
		}

		@Transactional("qualifiedTransactionManager")
		public void saveQualifiedFoo() {
		}

		@Transactional(transactionManager = "${myTransactionManager}")
		public void saveQualifiedFooWithAttributeAlias() {
		}
	}


	@Configuration
	static class PlaceholderConfig {

		@Bean
		public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
			PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
			Properties props = new Properties();
			props.setProperty("myLabel", "LABEL");
			props.setProperty("myTimeout", "5");
			props.setProperty("myTransactionManager", "qualifiedTransactionManager");
			pspc.setProperties(props);
			return pspc;
		}
	}


	@Configuration
	@EnableTransactionManagement
	@Import(PlaceholderConfig.class)
	static class EnableTxConfig {
	}


	@Configuration
	static class InheritedEnableTxConfig extends EnableTxConfig {
	}


	@Configuration
	@EnableTransactionManagement
	@Import(PlaceholderConfig.class)
	@Conditional(NeverCondition.class)
	static class ParentEnableTxConfig {

		@Bean
		Object someBean() {
			return new Object();
		}
	}


	@Configuration
	static class ChildEnableTxConfig extends ParentEnableTxConfig {

		@Override
		Object someBean() {
			return "X";
		}
	}


	private static class NeverCondition implements ConfigurationCondition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return false;
		}

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}
	}


	@Configuration
	@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
	static class EnableAspectjTxConfig {
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

		@Override
		public PlatformTransactionManager annotationDrivenTransactionManager() {
			return txManager2();
		}
	}


	@Configuration
	static class PrimaryMultiTxManagerConfig {

		@Bean
		public TransactionalTestBean testBean() {
			return new TransactionalTestBean();
		}

		@Bean
		@Primary
		public PlatformTransactionManager primary() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public PlatformTransactionManager txManager2() {
			return new CallCountingTransactionManager();
		}
	}


	@Configuration
	static class PrimaryTxManagerAndTxMgmtConfigurerConfig implements TransactionManagementConfigurer {

		@Bean
		public TransactionalTestBean testBean() {
			return new TransactionalTestBean();
		}

		@Bean
		@Primary
		public PlatformTransactionManager primary() {
			return new CallCountingTransactionManager();
		}

		@Bean
		@Override
		public PlatformTransactionManager annotationDrivenTransactionManager() {
			return new CallCountingTransactionManager();
		}
	}


	@Configuration
	static class SingleTxManagerBeanAndTxMgmtConfigurerConfig implements TransactionManagementConfigurer {

		final CallCountingTransactionManager annotationDriven = new CallCountingTransactionManager();

		@Bean
		public TransactionalTestBean testBean() {
			return new TransactionalTestBean();
		}

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}

		// The transaction manager returned from this method is intentionally not
		// registered as a bean in the ApplicationContext.
		@Override
		public PlatformTransactionManager annotationDrivenTransactionManager() {
			return annotationDriven;
		}
	}


	@Configuration
	@EnableTransactionManagement
	@Import(PlaceholderConfig.class)
	static class Spr11915Config {

		@Autowired
		public void initializeApp(ConfigurableApplicationContext applicationContext) {
			applicationContext.getBeanFactory().registerSingleton(
					"qualifiedTransactionManager", new CallCountingTransactionManager());
		}

		@Bean
		public TransactionalTestBean testBean() {
			return new TransactionalTestBean();
		}
	}


	public interface BaseTransactionalInterface {

		@Transactional
		default void saveBar() {
		}
	}


	public interface TransactionalTestInterface extends BaseTransactionalInterface {

		@Transactional
		void saveFoo();
	}


	@Service
	public static class TransactionalTestService implements TransactionalTestInterface {

		@Override
		public void saveFoo() {
		}
	}


	@Configuration
	@EnableTransactionManagement
	static class Spr14322ConfigA {

		@Bean
		public TransactionalTestInterface testBean() {
			return new TransactionalTestService();
		}

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}
	}


	@Configuration
	@EnableTransactionManagement(proxyTargetClass = true)
	static class Spr14322ConfigB {

		@Bean
		public TransactionalTestInterface testBean() {
			return new TransactionalTestService();
		}

		@Bean
		public PlatformTransactionManager txManager() {
			return new CallCountingTransactionManager();
		}
	}

}
