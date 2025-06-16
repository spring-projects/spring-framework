/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.context.support;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
class DefaultLifecycleProcessorTests {

	@Test
	void defaultLifecycleProcessorInstance() {
		StaticApplicationContext context = new StaticApplicationContext();
		context.refresh();
		Object lifecycleProcessor = new DirectFieldAccessor(context).getPropertyValue("lifecycleProcessor");
		assertThat(lifecycleProcessor).isNotNull();
		assertThat(lifecycleProcessor.getClass()).isEqualTo(DefaultLifecycleProcessor.class);
		context.close();
	}

	@Test
	void customLifecycleProcessorInstance() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(DefaultLifecycleProcessor.class);
		beanDefinition.getPropertyValues().addPropertyValue("timeoutPerShutdownPhase", 1000);
		context.registerBeanDefinition(StaticApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME, beanDefinition);

		context.refresh();
		LifecycleProcessor bean = context.getBean("lifecycleProcessor", LifecycleProcessor.class);
		Object contextLifecycleProcessor = new DirectFieldAccessor(context).getPropertyValue("lifecycleProcessor");
		assertThat(contextLifecycleProcessor).isNotNull();
		assertThat(contextLifecycleProcessor).isSameAs(bean);
		assertThat(new DirectFieldAccessor(contextLifecycleProcessor).getPropertyValue("timeoutPerShutdownPhase"))
				.isEqualTo(1000L);
		context.close();
	}

	@Test
	void singleSmartLifecycleAutoStartup() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		bean.setAutoStartup(true);
		context.getBeanFactory().registerSingleton("bean", bean);

		assertThat(bean.isRunning()).isFalse();
		context.refresh();
		assertThat(bean.isRunning()).isTrue();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		assertThat(startedBeans).hasSize(1);
		context.close();
	}

	@Test
	void singleSmartLifecycleAutoStartupWithLazyInit() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(DummySmartLifecycleBean.class);
		bd.setLazyInit(true);
		context.registerBeanDefinition("bean", bd);
		context.refresh();
		DummySmartLifecycleBean bean = context.getBean("bean", DummySmartLifecycleBean.class);
		assertThat(bean.isRunning()).isTrue();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		context.close();
	}

	@Test
	void singleSmartLifecycleAutoStartupWithLazyInitFactoryBean() {
		StaticApplicationContext context = new StaticApplicationContext();
		RootBeanDefinition bd = new RootBeanDefinition(DummySmartLifecycleFactoryBean.class);
		bd.setLazyInit(true);
		context.registerBeanDefinition("bean", bd);
		context.refresh();
		DummySmartLifecycleFactoryBean bean = context.getBean("&bean", DummySmartLifecycleFactoryBean.class);
		assertThat(bean.isRunning()).isTrue();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		context.close();
	}

	@Test
	void singleSmartLifecycleAutoStartupWithFailingLifecycleBean() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		bean.setAutoStartup(true);
		context.getBeanFactory().registerSingleton("bean", bean);
		context.registerSingleton("failingBean", FailingLifecycleBean.class);

		assertThat(bean.isRunning()).isFalse();
		assertThatExceptionOfType(ApplicationContextException.class)
				.isThrownBy(context::refresh).withCauseInstanceOf(IllegalStateException.class);
		assertThat(bean.isRunning()).isFalse();
		assertThat(startedBeans).hasSize(1);
		context.close();
	}

	@Test
	void singleSmartLifecycleWithoutAutoStartup() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		bean.setAutoStartup(false);
		context.getBeanFactory().registerSingleton("bean", bean);

		assertThat(bean.isRunning()).isFalse();
		context.refresh();
		assertThat(bean.isRunning()).isFalse();
		assertThat(startedBeans).isEmpty();
		context.start();
		assertThat(bean.isRunning()).isTrue();
		assertThat(startedBeans).hasSize(1);
		context.stop();
		context.close();
	}

	@Test
	void singleSmartLifecycleAutoStartupWithNonAutoStartupDependency() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		bean.setAutoStartup(true);
		TestSmartLifecycleBean dependency = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		dependency.setAutoStartup(false);
		context.getBeanFactory().registerSingleton("bean", bean);
		context.getBeanFactory().registerSingleton("dependency", dependency);
		context.getBeanFactory().registerDependentBean("dependency", "bean");

		assertThat(bean.isRunning()).isFalse();
		assertThat(dependency.isRunning()).isFalse();
		context.refresh();
		assertThat(bean.isRunning()).isTrue();
		assertThat(dependency.isRunning()).isFalse();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		assertThat(dependency.isRunning()).isFalse();
		assertThat(startedBeans).hasSize(1);
		context.close();
	}

	@Test
	void singleSmartLifecycleAutoStartupWithBootstrapExecutor() {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = new RootBeanDefinition(DefaultLifecycleProcessor.class);
		beanDefinition.getPropertyValues().addPropertyValue("concurrentStartupForPhases", Map.of(1, 1000));
		context.registerBeanDefinition(StaticApplicationContext.LIFECYCLE_PROCESSOR_BEAN_NAME, beanDefinition);
		context.registerSingleton(StaticApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME, ThreadPoolTaskExecutor.class);

		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		bean.setAutoStartup(true);
		context.getBeanFactory().registerSingleton("bean", bean);
		assertThat(bean.isRunning()).isFalse();
		context.refresh();
		assertThat(bean.isRunning()).isTrue();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		assertThat(startedBeans).hasSize(1);
		context.close();
	}

	@Test
	void smartLifecycleGroupStartup() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean beanMin = TestSmartLifecycleBean.forStartupTests(Integer.MIN_VALUE, startedBeans);
		TestSmartLifecycleBean bean1 = TestSmartLifecycleBean.forStartupTests(1, startedBeans);
		TestSmartLifecycleBean bean2 = TestSmartLifecycleBean.forStartupTests(2, startedBeans);
		TestSmartLifecycleBean bean3 = TestSmartLifecycleBean.forStartupTests(3, startedBeans);
		TestSmartLifecycleBean beanMax = TestSmartLifecycleBean.forStartupTests(Integer.MAX_VALUE, startedBeans);
		context.getBeanFactory().registerSingleton("bean3", bean3);
		context.getBeanFactory().registerSingleton("beanMin", beanMin);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("beanMax", beanMax);
		context.getBeanFactory().registerSingleton("bean1", bean1);

		assertThat(beanMin.isRunning()).isFalse();
		assertThat(bean1.isRunning()).isFalse();
		assertThat(bean2.isRunning()).isFalse();
		assertThat(bean3.isRunning()).isFalse();
		assertThat(beanMax.isRunning()).isFalse();
		context.refresh();
		assertThat(beanMin.isRunning()).isTrue();
		assertThat(bean1.isRunning()).isTrue();
		assertThat(bean2.isRunning()).isTrue();
		assertThat(bean3.isRunning()).isTrue();
		assertThat(beanMax.isRunning()).isTrue();
		context.stop();
		assertThat(startedBeans).satisfiesExactly(hasPhase(Integer.MIN_VALUE),hasPhase(1),
				hasPhase(2), hasPhase(3), hasPhase(Integer.MAX_VALUE));
		context.close();
	}

	@Test
	void contextRefreshThenStartWithMixedBeans() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestLifecycleBean simpleBean1 = TestLifecycleBean.forStartupTests(startedBeans);
		TestLifecycleBean simpleBean2 = TestLifecycleBean.forStartupTests(startedBeans);
		TestSmartLifecycleBean smartBean1 = TestSmartLifecycleBean.forStartupTests(5, startedBeans);
		TestSmartLifecycleBean smartBean2 = TestSmartLifecycleBean.forStartupTests(-3, startedBeans);
		context.getBeanFactory().registerSingleton("simpleBean1", simpleBean1);
		context.getBeanFactory().registerSingleton("smartBean1", smartBean1);
		context.getBeanFactory().registerSingleton("simpleBean2", simpleBean2);
		context.getBeanFactory().registerSingleton("smartBean2", smartBean2);

		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(smartBean1.isRunning()).isFalse();
		assertThat(smartBean2.isRunning()).isFalse();
		context.refresh();
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isTrue();
		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5));
		context.start();
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isTrue();
		assertThat(simpleBean1.isRunning()).isTrue();
		assertThat(simpleBean2.isRunning()).isTrue();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5), hasPhase(0), hasPhase(0));
		context.close();
	}

	@Test
	void contextRefreshThenStopAndRestartWithMixedBeans() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestLifecycleBean simpleBean1 = TestLifecycleBean.forStartupTests(startedBeans);
		TestLifecycleBean simpleBean2 = TestLifecycleBean.forStartupTests(startedBeans);
		TestSmartLifecycleBean smartBean1 = TestSmartLifecycleBean.forStartupTests(5, startedBeans);
		TestSmartLifecycleBean smartBean2 = TestSmartLifecycleBean.forStartupTests(-3, startedBeans);
		context.getBeanFactory().registerSingleton("simpleBean1", simpleBean1);
		context.getBeanFactory().registerSingleton("smartBean1", smartBean1);
		context.getBeanFactory().registerSingleton("simpleBean2", simpleBean2);
		context.getBeanFactory().registerSingleton("smartBean2", smartBean2);

		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(smartBean1.isRunning()).isFalse();
		assertThat(smartBean2.isRunning()).isFalse();
		context.refresh();
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isTrue();
		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5));
		context.stop();
		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(smartBean1.isRunning()).isFalse();
		assertThat(smartBean2.isRunning()).isFalse();
		context.start();
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isTrue();
		assertThat(simpleBean1.isRunning()).isTrue();
		assertThat(simpleBean2.isRunning()).isTrue();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5),
				hasPhase(-3), hasPhase(0), hasPhase(0), hasPhase(5));
		context.close();
	}

	@Test
	void contextRefreshThenStopForRestartWithMixedBeans() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestLifecycleBean simpleBean1 = TestLifecycleBean.forStartupTests(startedBeans);
		TestLifecycleBean simpleBean2 = TestLifecycleBean.forStartupTests(startedBeans);
		TestSmartLifecycleBean smartBean1 = TestSmartLifecycleBean.forStartupTests(5, startedBeans);
		TestSmartLifecycleBean smartBean2 = TestSmartLifecycleBean.forStartupTests(-3, startedBeans);
		context.getBeanFactory().registerSingleton("simpleBean1", simpleBean1);
		context.getBeanFactory().registerSingleton("smartBean1", smartBean1);
		context.getBeanFactory().registerSingleton("simpleBean2", simpleBean2);
		context.getBeanFactory().registerSingleton("smartBean2", smartBean2);

		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(smartBean1.isRunning()).isFalse();
		assertThat(smartBean2.isRunning()).isFalse();
		context.refresh();
		DefaultLifecycleProcessor lifecycleProcessor = (DefaultLifecycleProcessor)
				new DirectFieldAccessor(context).getPropertyValue("lifecycleProcessor");
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isTrue();
		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		smartBean2.stop();
		simpleBean1.start();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5), hasPhase(0));
		lifecycleProcessor.stopForRestart();
		assertThat(simpleBean1.isRunning()).isFalse();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(smartBean1.isRunning()).isFalse();
		assertThat(smartBean2.isRunning()).isFalse();
		lifecycleProcessor.restartAfterStop();
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isFalse();
		assertThat(simpleBean1.isRunning()).isTrue();
		assertThat(simpleBean2.isRunning()).isFalse();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5),
				hasPhase(0), hasPhase(0), hasPhase(5));
		context.start();
		assertThat(smartBean1.isRunning()).isTrue();
		assertThat(smartBean2.isRunning()).isTrue();
		assertThat(simpleBean1.isRunning()).isTrue();
		assertThat(simpleBean2.isRunning()).isTrue();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-3), hasPhase(5),
				hasPhase(0), hasPhase(0), hasPhase(5), hasPhase(-3), hasPhase(0));
		context.close();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void smartLifecycleGroupShutdown() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean1 = TestSmartLifecycleBean.forShutdownTests(1, 300, stoppedBeans);
		TestSmartLifecycleBean bean2 = TestSmartLifecycleBean.forShutdownTests(3, 100, stoppedBeans);
		TestSmartLifecycleBean bean3 = TestSmartLifecycleBean.forShutdownTests(1, 600, stoppedBeans);
		TestSmartLifecycleBean bean4 = TestSmartLifecycleBean.forShutdownTests(2, 400, stoppedBeans);
		TestSmartLifecycleBean bean5 = TestSmartLifecycleBean.forShutdownTests(2, 700, stoppedBeans);
		TestSmartLifecycleBean bean6 = TestSmartLifecycleBean.forShutdownTests(Integer.MAX_VALUE, 200, stoppedBeans);
		TestSmartLifecycleBean bean7 = TestSmartLifecycleBean.forShutdownTests(3, 200, stoppedBeans);
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean3", bean3);
		context.getBeanFactory().registerSingleton("bean4", bean4);
		context.getBeanFactory().registerSingleton("bean5", bean5);
		context.getBeanFactory().registerSingleton("bean6", bean6);
		context.getBeanFactory().registerSingleton("bean7", bean7);

		context.refresh();
		context.stop();
		assertThat(stoppedBeans).satisfiesExactly(hasPhase(Integer.MAX_VALUE), hasPhase(3),
				hasPhase(3), hasPhase(2), hasPhase(2), hasPhase(1), hasPhase(1));
		context.close();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void singleSmartLifecycleShutdown() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean = TestSmartLifecycleBean.forShutdownTests(99, 300, stoppedBeans);
		context.getBeanFactory().registerSingleton("bean", bean);
		context.refresh();

		assertThat(bean.isRunning()).isTrue();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		assertThat(stoppedBeans).containsExactly(bean);
		context.close();
	}

	@Test
	void singleLifecycleShutdown() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		Lifecycle bean = new TestLifecycleBean(null, stoppedBeans);
		context.getBeanFactory().registerSingleton("bean", bean);

		context.refresh();
		assertThat(bean.isRunning()).isFalse();
		bean.start();
		assertThat(bean.isRunning()).isTrue();
		context.stop();
		assertThat(bean.isRunning()).isFalse();
		assertThat(stoppedBeans).singleElement().isEqualTo(bean);
		context.close();
	}

	@Test
	void mixedShutdown() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		Lifecycle bean1 = TestLifecycleBean.forShutdownTests(stoppedBeans);
		Lifecycle bean2 = TestSmartLifecycleBean.forShutdownTests(500, 200, stoppedBeans);
		Lifecycle bean3 = TestSmartLifecycleBean.forShutdownTests(Integer.MAX_VALUE, 100, stoppedBeans);
		Lifecycle bean4 = TestLifecycleBean.forShutdownTests(stoppedBeans);
		Lifecycle bean5 = TestSmartLifecycleBean.forShutdownTests(1, 200, stoppedBeans);
		Lifecycle bean6 = TestSmartLifecycleBean.forShutdownTests(-1, 100, stoppedBeans);
		Lifecycle bean7 = TestSmartLifecycleBean.forShutdownTests(Integer.MIN_VALUE, 300, stoppedBeans);
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean3", bean3);
		context.getBeanFactory().registerSingleton("bean4", bean4);
		context.getBeanFactory().registerSingleton("bean5", bean5);
		context.getBeanFactory().registerSingleton("bean6", bean6);
		context.getBeanFactory().registerSingleton("bean7", bean7);

		context.refresh();
		assertThat(bean2.isRunning()).isTrue();
		assertThat(bean3.isRunning()).isTrue();
		assertThat(bean5.isRunning()).isTrue();
		assertThat(bean6.isRunning()).isTrue();
		assertThat(bean7.isRunning()).isTrue();
		assertThat(bean1.isRunning()).isFalse();
		assertThat(bean4.isRunning()).isFalse();
		bean1.start();
		bean4.start();
		assertThat(bean1.isRunning()).isTrue();
		assertThat(bean4.isRunning()).isTrue();
		context.stop();
		assertThat(bean1.isRunning()).isFalse();
		assertThat(bean2.isRunning()).isFalse();
		assertThat(bean3.isRunning()).isFalse();
		assertThat(bean4.isRunning()).isFalse();
		assertThat(bean5.isRunning()).isFalse();
		assertThat(bean6.isRunning()).isFalse();
		assertThat(bean7.isRunning()).isFalse();
		assertThat(stoppedBeans).satisfiesExactly(hasPhase(Integer.MAX_VALUE), hasPhase(500),
				hasPhase(1), hasPhase(0), hasPhase(0), hasPhase(-1), hasPhase(Integer.MIN_VALUE));
		context.close();
	}

	@Test
	void dependencyStartedFirstEvenIfItsPhaseIsHigher() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean beanMin = TestSmartLifecycleBean.forStartupTests(Integer.MIN_VALUE, startedBeans);
		TestSmartLifecycleBean bean2 = TestSmartLifecycleBean.forStartupTests(2, startedBeans);
		TestSmartLifecycleBean bean99 = TestSmartLifecycleBean.forStartupTests(99, startedBeans);
		TestSmartLifecycleBean beanMax = TestSmartLifecycleBean.forStartupTests(Integer.MAX_VALUE, startedBeans);
		context.getBeanFactory().registerSingleton("beanMin", beanMin);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean99", bean99);
		context.getBeanFactory().registerSingleton("beanMax", beanMax);
		context.getBeanFactory().registerDependentBean("bean99", "bean2");

		context.refresh();
		assertThat(beanMin.isRunning()).isTrue();
		assertThat(bean2.isRunning()).isTrue();
		assertThat(bean99.isRunning()).isTrue();
		assertThat(beanMax.isRunning()).isTrue();
		assertThat(startedBeans).satisfiesExactly(
				hasPhase(Integer.MIN_VALUE),
				one -> assertThat(one).isEqualTo(bean99).satisfies(hasPhase(99)),
				two -> assertThat(two).isEqualTo(bean2).satisfies(hasPhase(2)),
				hasPhase(Integer.MAX_VALUE));
		context.stop();
		context.close();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void dependentShutdownFirstEvenIfItsPhaseIsLower() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean beanMin = TestSmartLifecycleBean.forShutdownTests(Integer.MIN_VALUE, 100, stoppedBeans);
		TestSmartLifecycleBean bean1 = TestSmartLifecycleBean.forShutdownTests(1, 200, stoppedBeans);
		TestSmartLifecycleBean bean99 = TestSmartLifecycleBean.forShutdownTests(99, 100, stoppedBeans);
		TestSmartLifecycleBean bean2 = TestSmartLifecycleBean.forShutdownTests(2, 300, stoppedBeans);
		TestSmartLifecycleBean bean7 = TestSmartLifecycleBean.forShutdownTests(7, 400, stoppedBeans);
		TestSmartLifecycleBean beanMax = TestSmartLifecycleBean.forShutdownTests(Integer.MAX_VALUE, 400, stoppedBeans);
		context.getBeanFactory().registerSingleton("beanMin", beanMin);
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("bean99", bean99);
		context.getBeanFactory().registerSingleton("beanMax", beanMax);
		context.getBeanFactory().registerDependentBean("bean99", "bean2");

		context.refresh();
		assertThat(beanMin.isRunning()).isTrue();
		assertThat(bean1.isRunning()).isTrue();
		assertThat(bean2.isRunning()).isTrue();
		assertThat(bean7.isRunning()).isTrue();
		assertThat(bean99.isRunning()).isTrue();
		assertThat(beanMax.isRunning()).isTrue();
		context.stop();
		assertThat(beanMin.isRunning()).isFalse();
		assertThat(bean1.isRunning()).isFalse();
		assertThat(bean2.isRunning()).isFalse();
		assertThat(bean7.isRunning()).isFalse();
		assertThat(bean99.isRunning()).isFalse();
		assertThat(beanMax.isRunning()).isFalse();
		assertThat(stoppedBeans).hasSize(6);
		assertThat(stoppedBeans).satisfiesExactly(
				hasPhase(Integer.MAX_VALUE),
				one -> assertThat(one).isEqualTo(bean2).satisfies(hasPhase(2)),
				two -> assertThat(two).isEqualTo(bean99).satisfies(hasPhase(99)),
				hasPhase(7), hasPhase(1), hasPhase(Integer.MIN_VALUE));
		context.close();
	}

	@Test
	void dependencyStartedFirstAndIsSmartLifecycle() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean beanNegative = TestSmartLifecycleBean.forStartupTests(-99, startedBeans);
		TestSmartLifecycleBean bean99 = TestSmartLifecycleBean.forStartupTests(99, startedBeans);
		TestSmartLifecycleBean bean7 = TestSmartLifecycleBean.forStartupTests(7, startedBeans);
		TestLifecycleBean simpleBean = TestLifecycleBean.forStartupTests(startedBeans);
		context.getBeanFactory().registerSingleton("beanNegative", beanNegative);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("bean99", bean99);
		context.getBeanFactory().registerSingleton("simpleBean", simpleBean);
		context.getBeanFactory().registerDependentBean("bean7", "simpleBean");

		context.refresh();
		context.stop();
		startedBeans.clear();
		// clean start so that simpleBean is included
		context.start();
		assertThat(beanNegative.isRunning()).isTrue();
		assertThat(bean99.isRunning()).isTrue();
		assertThat(bean7.isRunning()).isTrue();
		assertThat(simpleBean.isRunning()).isTrue();
		assertThat(startedBeans).satisfiesExactly(hasPhase(-99), hasPhase(7), hasPhase(0), hasPhase(99));
		context.stop();
		context.close();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void dependentShutdownFirstAndIsSmartLifecycle() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean beanMin = TestSmartLifecycleBean.forShutdownTests(Integer.MIN_VALUE, 400, stoppedBeans);
		TestSmartLifecycleBean beanNegative = TestSmartLifecycleBean.forShutdownTests(-99, 100, stoppedBeans);
		TestSmartLifecycleBean bean1 = TestSmartLifecycleBean.forShutdownTests(1, 200, stoppedBeans);
		TestSmartLifecycleBean bean2 = TestSmartLifecycleBean.forShutdownTests(2, 300, stoppedBeans);
		TestSmartLifecycleBean bean7 = TestSmartLifecycleBean.forShutdownTests(7, 400, stoppedBeans);
		TestLifecycleBean simpleBean = TestLifecycleBean.forShutdownTests(stoppedBeans);
		context.getBeanFactory().registerSingleton("beanMin", beanMin);
		context.getBeanFactory().registerSingleton("beanNegative", beanNegative);
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("simpleBean", simpleBean);
		context.getBeanFactory().registerDependentBean("simpleBean", "beanNegative");

		context.refresh();
		assertThat(beanMin.isRunning()).isTrue();
		assertThat(beanNegative.isRunning()).isTrue();
		assertThat(bean1.isRunning()).isTrue();
		assertThat(bean2.isRunning()).isTrue();
		assertThat(bean7.isRunning()).isTrue();
		// should start since it's a dependency of an auto-started bean
		assertThat(simpleBean.isRunning()).isTrue();
		context.stop();
		assertThat(beanMin.isRunning()).isFalse();
		assertThat(beanNegative.isRunning()).isFalse();
		assertThat(bean1.isRunning()).isFalse();
		assertThat(bean2.isRunning()).isFalse();
		assertThat(bean7.isRunning()).isFalse();
		assertThat(simpleBean.isRunning()).isFalse();
		assertThat(stoppedBeans).satisfiesExactly(hasPhase(7), hasPhase(2),
				hasPhase(1), hasPhase(-99), hasPhase(0), hasPhase(Integer.MIN_VALUE));
		context.close();
	}

	@Test
	void dependencyStartedFirstButNotSmartLifecycle() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> startedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean beanMin = TestSmartLifecycleBean.forStartupTests(Integer.MIN_VALUE, startedBeans);
		TestSmartLifecycleBean bean7 = TestSmartLifecycleBean.forStartupTests(7, startedBeans);
		TestLifecycleBean simpleBean = TestLifecycleBean.forStartupTests(startedBeans);
		context.getBeanFactory().registerSingleton("beanMin", beanMin);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("simpleBean", simpleBean);
		context.getBeanFactory().registerDependentBean("simpleBean", "beanMin");

		context.refresh();
		assertThat(beanMin.isRunning()).isTrue();
		assertThat(bean7.isRunning()).isTrue();
		assertThat(simpleBean.isRunning()).isTrue();
		assertThat(startedBeans).satisfiesExactly(hasPhase(0), hasPhase(Integer.MIN_VALUE), hasPhase(7));
		context.stop();
		context.close();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void dependentShutdownFirstButNotSmartLifecycle() {
		StaticApplicationContext context = new StaticApplicationContext();
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<>();
		TestSmartLifecycleBean bean1 = TestSmartLifecycleBean.forShutdownTests(1, 200, stoppedBeans);
		TestLifecycleBean simpleBean = TestLifecycleBean.forShutdownTests(stoppedBeans);
		TestSmartLifecycleBean bean2 = TestSmartLifecycleBean.forShutdownTests(2, 300, stoppedBeans);
		TestSmartLifecycleBean bean7 = TestSmartLifecycleBean.forShutdownTests(7, 400, stoppedBeans);
		TestSmartLifecycleBean beanMin = TestSmartLifecycleBean.forShutdownTests(Integer.MIN_VALUE, 400, stoppedBeans);
		context.getBeanFactory().registerSingleton("beanMin", beanMin);
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("simpleBean", simpleBean);
		context.getBeanFactory().registerDependentBean("bean2", "simpleBean");

		context.refresh();
		assertThat(beanMin.isRunning()).isTrue();
		assertThat(bean1.isRunning()).isTrue();
		assertThat(bean2.isRunning()).isTrue();
		assertThat(bean7.isRunning()).isTrue();
		assertThat(simpleBean.isRunning()).isFalse();
		simpleBean.start();
		assertThat(simpleBean.isRunning()).isTrue();
		context.stop();
		assertThat(beanMin.isRunning()).isFalse();
		assertThat(bean1.isRunning()).isFalse();
		assertThat(bean2.isRunning()).isFalse();
		assertThat(bean7.isRunning()).isFalse();
		assertThat(simpleBean.isRunning()).isFalse();
		assertThat(stoppedBeans).satisfiesExactly(hasPhase(7), hasPhase(0),
				hasPhase(2), hasPhase(1), hasPhase(Integer.MIN_VALUE));
		context.close();
	}

	private Consumer<? super Lifecycle> hasPhase(int phase) {
		return lifecycle -> {
			int actual = lifecycle instanceof SmartLifecycle smartLifecycle ? smartLifecycle.getPhase() : 0;
			assertThat(actual).isEqualTo(phase);
		};
	}


	private static class TestLifecycleBean implements Lifecycle {

		private final CopyOnWriteArrayList<Lifecycle> startedBeans;

		private final CopyOnWriteArrayList<Lifecycle> stoppedBeans;

		private volatile boolean running;

		static TestLifecycleBean forStartupTests(CopyOnWriteArrayList<Lifecycle> startedBeans) {
			return new TestLifecycleBean(startedBeans, null);
		}

		static TestLifecycleBean forShutdownTests(CopyOnWriteArrayList<Lifecycle> stoppedBeans) {
			return new TestLifecycleBean(null, stoppedBeans);
		}

		private TestLifecycleBean(CopyOnWriteArrayList<Lifecycle> startedBeans, CopyOnWriteArrayList<Lifecycle> stoppedBeans) {
			this.startedBeans = startedBeans;
			this.stoppedBeans = stoppedBeans;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public void start() {
			if (this.startedBeans != null) {
				this.startedBeans.add(this);
			}
			this.running = true;
		}

		@Override
		public void stop() {
			if (this.stoppedBeans != null) {
				this.stoppedBeans.add(this);
			}
			this.running = false;
		}
	}


	private static class TestSmartLifecycleBean extends TestLifecycleBean implements SmartLifecycle {

		private final int phase;

		private final int shutdownDelay;

		private volatile boolean autoStartup = true;

		static TestSmartLifecycleBean forStartupTests(int phase, CopyOnWriteArrayList<Lifecycle> startedBeans) {
			return new TestSmartLifecycleBean(phase, 0, startedBeans, null);
		}

		static TestSmartLifecycleBean forShutdownTests(int phase, int shutdownDelay, CopyOnWriteArrayList<Lifecycle> stoppedBeans) {
			return new TestSmartLifecycleBean(phase, shutdownDelay, null, stoppedBeans);
		}

		private TestSmartLifecycleBean(int phase, int shutdownDelay, CopyOnWriteArrayList<Lifecycle> startedBeans,
				CopyOnWriteArrayList<Lifecycle> stoppedBeans) {
			super(startedBeans, stoppedBeans);
			this.phase = phase;
			this.shutdownDelay = shutdownDelay;
		}

		@Override
		public int getPhase() {
			return this.phase;
		}

		@Override
		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		@Override
		public void stop(final Runnable callback) {
			// calling stop() before the delay to preserve
			// invocation order in the 'stoppedBeans' list
			stop();
			final int delay = this.shutdownDelay;
			new Thread(() -> {
				try {
					Thread.sleep(delay);
				}
				catch (InterruptedException e) {
					// ignore
				}
				finally {
					callback.run();
				}
			}).start();
		}
	}


	public static class DummySmartLifecycleBean implements SmartLifecycle {

		public boolean running = false;

		@Override
		public boolean isAutoStartup() {
			return true;
		}

		@Override
		public void stop(Runnable callback) {
			this.running = false;
			callback.run();
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public int getPhase() {
			return 0;
		}
	}


	public static class DummySmartLifecycleFactoryBean implements FactoryBean<Object>, SmartLifecycle {

		public boolean running = false;

		DummySmartLifecycleBean bean = new DummySmartLifecycleBean();

		@Override
		public Object getObject() {
			return this.bean;
		}

		@Override
		public Class<?> getObjectType() {
			return DummySmartLifecycleBean.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public boolean isAutoStartup() {
			return true;
		}

		@Override
		public void stop(Runnable callback) {
			this.running = false;
			callback.run();
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			this.running = false;
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public int getPhase() {
			return 0;
		}
	}


	public static class FailingLifecycleBean implements SmartLifecycle {

		@Override
		public void start() {
			throw new IllegalStateException();
		}

		@Override
		public void stop() {
			throw new IllegalStateException();
		}

		@Override
		public boolean isRunning() {
			return false;
		}
	}

}
