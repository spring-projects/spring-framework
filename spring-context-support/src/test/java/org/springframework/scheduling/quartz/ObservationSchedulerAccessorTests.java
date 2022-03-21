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

package org.springframework.scheduling.quartz;

import java.util.HashMap;
import java.util.Map;

import io.micrometer.observation.ObservationRegistry;
import org.mockito.BDDMockito;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerFactory;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;
import org.springframework.transaction.testfixture.ObservationTransactionManagerSampleTestRunner;

import static org.mockito.Mockito.mock;

/**
 * @author Marcin Grzejszczak
 */
class ObservationSchedulerAccessorTests extends ObservationTransactionManagerSampleTestRunner<SchedulerFactoryBean> {

	@Override
	protected SchedulerFactoryBean given(ObservationRegistry observationRegistry) throws Exception {
		TestBean tb = new TestBean("tb", 99);
		StaticApplicationContext ac = new StaticApplicationContext();
		final Scheduler scheduler = mock(Scheduler.class);
		SchedulerContext schedulerContext = new SchedulerContext();
		BDDMockito.given(scheduler.getContext()).willReturn(schedulerContext);
		return schedulerFactoryBean(observationRegistry, tb, ac, scheduler);
	}

	private SchedulerFactoryBean schedulerFactoryBean(ObservationRegistry observationRegistry, TestBean tb, StaticApplicationContext ac, Scheduler scheduler) {
		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean() {
			@Override
			protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) {
				return scheduler;
			}
		};
		schedulerFactoryBean.setJobFactory(null);
		Map<String, Object> schedulerContextMap = new HashMap<>();
		schedulerContextMap.put("testBean", tb);
		schedulerFactoryBean.setSchedulerContextAsMap(schedulerContextMap);
		schedulerFactoryBean.setApplicationContext(ac);
		schedulerFactoryBean.setApplicationContextSchedulerContextKey("appCtx");
		schedulerFactoryBean.setTransactionManager(new CallCountingTransactionManager());
		schedulerFactoryBean.setObservationRegistry(observationRegistry);
		return schedulerFactoryBean;
	}

	@Override
	protected void when(SchedulerFactoryBean sut) throws Exception {
		sut.afterPropertiesSet();
		sut.start();
	}
}
