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

package org.springframework.scheduling.quartz;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerFactory;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.SchedulerRepository;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.core.testfixture.TestGroup.PERFORMANCE;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Dave Syer
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 20.02.2004
 */
public class QuartzSupportTests {

	@Test
	public void schedulerFactoryBeanWithApplicationContext() throws Exception {
		TestBean tb = new TestBean("tb", 99);
		StaticApplicationContext ac = new StaticApplicationContext();

		final Scheduler scheduler = mock(Scheduler.class);
		SchedulerContext schedulerContext = new SchedulerContext();
		given(scheduler.getContext()).willReturn(schedulerContext);

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
		try {
			schedulerFactoryBean.afterPropertiesSet();
			schedulerFactoryBean.start();
			Scheduler returnedScheduler = schedulerFactoryBean.getObject();
			assertThat(returnedScheduler.getContext().get("testBean")).isEqualTo(tb);
			assertThat(returnedScheduler.getContext().get("appCtx")).isEqualTo(ac);
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerWithTaskExecutor() throws Exception {
		CountingTaskExecutor taskExecutor = new CountingTaskExecutor();
		DummyJob.count = 0;

		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setDurability(true);
		jobDetail.setJobClass(DummyJob.class);
		jobDetail.setName("myJob");

		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTaskExecutor(taskExecutor);
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJob.count > 0).as("DummyJob should have been executed at least once.").isTrue();
		assertThat(taskExecutor.count).isEqualTo(DummyJob.count);

		bean.destroy();
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void jobDetailWithRunnableInsteadOfJob() {
		JobDetailImpl jobDetail = new JobDetailImpl();
		assertThatIllegalArgumentException().isThrownBy(() ->
				jobDetail.setJobClass((Class) DummyRunnable.class));
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerWithQuartzJobBean() throws Exception {
		DummyJob.param = 0;
		DummyJob.count = 0;

		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setDurability(true);
		jobDetail.setJobClass(DummyJobBean.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");

		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJobBean.param).isEqualTo(10);
		assertThat(DummyJobBean.count > 0).isTrue();

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerWithSpringBeanJobFactory() throws Exception {
		DummyJob.param = 0;
		DummyJob.count = 0;

		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setDurability(true);
		jobDetail.setJobClass(DummyJob.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");
		jobDetail.getJobDataMap().put("ignoredParam", "10");

		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJob.param).isEqualTo(10);
		assertThat(DummyJob.count > 0).as("DummyJob should have been executed at least once.").isTrue();

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerWithSpringBeanJobFactoryAndParamMismatchNotIgnored() throws Exception {
		DummyJob.param = 0;
		DummyJob.count = 0;

		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setDurability(true);
		jobDetail.setJobClass(DummyJob.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("para", "10");
		jobDetail.getJobDataMap().put("ignoredParam", "10");

		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
		jobFactory.setIgnoredUnknownProperties("ignoredParam");
		bean.setJobFactory(jobFactory);
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();

		Thread.sleep(500);
		assertThat(DummyJob.param).isEqualTo(0);
		assertThat(DummyJob.count == 0).isTrue();

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerWithSpringBeanJobFactoryAndQuartzJobBean() throws Exception {
		DummyJobBean.param = 0;
		DummyJobBean.count = 0;

		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setDurability(true);
		jobDetail.setJobClass(DummyJobBean.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");

		SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJobBean.param).isEqualTo(10);
		assertThat(DummyJobBean.count > 0).isTrue();

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerWithSpringBeanJobFactoryAndJobSchedulingData() throws Exception {
		DummyJob.param = 0;
		DummyJob.count = 0;

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setJobSchedulingDataLocation("org/springframework/scheduling/quartz/job-scheduling-data.xml");
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJob.param).isEqualTo(10);
		assertThat(DummyJob.count > 0).as("DummyJob should have been executed at least once.").isTrue();

		bean.destroy();
	}

	@Test  // SPR-772
	public void multipleSchedulers() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("multipleSchedulers.xml")) {
			Scheduler scheduler1 = (Scheduler) ctx.getBean("scheduler1");
			Scheduler scheduler2 = (Scheduler) ctx.getBean("scheduler2");
			assertThat(scheduler2).isNotSameAs(scheduler1);
			assertThat(scheduler1.getSchedulerName()).isEqualTo("quartz1");
			assertThat(scheduler2.getSchedulerName()).isEqualTo("quartz2");
		}
	}

	@Test  // SPR-16884
	public void multipleSchedulersWithQuartzProperties() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("multipleSchedulersWithQuartzProperties.xml")) {
			Scheduler scheduler1 = (Scheduler) ctx.getBean("scheduler1");
			Scheduler scheduler2 = (Scheduler) ctx.getBean("scheduler2");
			assertThat(scheduler2).isNotSameAs(scheduler1);
			assertThat(scheduler1.getSchedulerName()).isEqualTo("quartz1");
			assertThat(scheduler2.getSchedulerName()).isEqualTo("quartz2");
		}
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void twoAnonymousMethodInvokingJobDetailFactoryBeans() throws Exception {
		Thread.sleep(3000);
		try (ClassPathXmlApplicationContext ctx = context("multipleAnonymousMethodInvokingJobDetailFB.xml")) {
			QuartzTestBean exportService = (QuartzTestBean) ctx.getBean("exportService");
			QuartzTestBean importService = (QuartzTestBean) ctx.getBean("importService");

			assertThat(exportService.getImportCount()).as("doImport called exportService").isEqualTo(0);
			assertThat(exportService.getExportCount()).as("doExport not called on exportService").isEqualTo(2);
			assertThat(importService.getImportCount()).as("doImport not called on importService").isEqualTo(2);
			assertThat(importService.getExportCount()).as("doExport called on importService").isEqualTo(0);
		}
	}

	@Test
	@EnabledForTestGroups(PERFORMANCE)
	public void schedulerAccessorBean() throws Exception {
		Thread.sleep(3000);
		try (ClassPathXmlApplicationContext ctx = context("schedulerAccessorBean.xml")) {
			QuartzTestBean exportService = (QuartzTestBean) ctx.getBean("exportService");
			QuartzTestBean importService = (QuartzTestBean) ctx.getBean("importService");

			assertThat(exportService.getImportCount()).as("doImport called exportService").isEqualTo(0);
			assertThat(exportService.getExportCount()).as("doExport not called on exportService").isEqualTo(2);
			assertThat(importService.getImportCount()).as("doImport not called on importService").isEqualTo(2);
			assertThat(importService.getExportCount()).as("doExport called on importService").isEqualTo(0);
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void schedulerAutoStartsOnContextRefreshedEventByDefault() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("scheduler", new RootBeanDefinition(SchedulerFactoryBean.class));
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertThat(bean.isStarted()).isFalse();
		context.refresh();
		assertThat(bean.isStarted()).isTrue();
	}

	@Test
	@SuppressWarnings("resource")
	public void schedulerAutoStartupFalse() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class)
				.addPropertyValue("autoStartup", false).getBeanDefinition();
		context.registerBeanDefinition("scheduler", beanDefinition);
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertThat(bean.isStarted()).isFalse();
		context.refresh();
		assertThat(bean.isStarted()).isFalse();
	}

	@Test
	public void schedulerRepositoryExposure() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("schedulerRepositoryExposure.xml")) {
			assertThat(ctx.getBean("scheduler")).isSameAs(SchedulerRepository.getInstance().lookup("myScheduler"));
		}
	}

	/**
	 * SPR-6038: detect HSQL and stop illegal locks being taken.
	 * TODO: Against Quartz 2.2, this test's job doesn't actually execute anymore...
	 */
	@Test
	public void schedulerWithHsqlDataSource() throws Exception {
		DummyJob.param = 0;
		DummyJob.count = 0;

		try (ClassPathXmlApplicationContext ctx = context("databasePersistence.xml")) {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
			assertThat(jdbcTemplate.queryForList("SELECT * FROM qrtz_triggers").isEmpty()).as("No triggers were persisted").isFalse();

			/*
				Thread.sleep(3000);
				assertTrue("DummyJob should have been executed at least once.", DummyJob.count > 0);
			 */
		}
	}

	private ClassPathXmlApplicationContext context(String path) {
		return new ClassPathXmlApplicationContext(path, getClass());
	}


	public static class CountingTaskExecutor implements TaskExecutor {

		private int count;

		@Override
		public void execute(Runnable task) {
			this.count++;
			task.run();
		}
	}


	public static class DummyJob implements Job {

		private static int param;

		private static int count;

		public void setParam(int value) {
			if (param > 0) {
				throw new IllegalStateException("Param already set");
			}
			param = value;
		}

		@Override
		public synchronized void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
			count++;
		}
	}


	public static class DummyJobBean extends QuartzJobBean {

		private static int param;

		private static int count;

		public void setParam(int value) {
			if (param > 0) {
				throw new IllegalStateException("Param already set");
			}
			param = value;
		}

		@Override
		protected synchronized void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
			count++;
		}
	}


	public static class DummyRunnable implements Runnable {

		@Override
		public void run() {
			/* no-op */
		}
	}

}
