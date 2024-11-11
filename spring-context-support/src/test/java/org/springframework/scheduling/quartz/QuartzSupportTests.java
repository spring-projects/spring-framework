/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerFactory;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.jdbcjobstore.JobStoreTX;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Dave Syer
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 20.02.2004
 */
class QuartzSupportTests {

	@Test
	void schedulerFactoryBeanWithApplicationContext() throws Exception {
		TestBean tb = new TestBean("tb", 99);
		StaticApplicationContext ac = new StaticApplicationContext();

		final Scheduler scheduler = mock();
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
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerWithTaskExecutor() throws Exception {
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
		trigger.setRepeatInterval(100);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTaskExecutor(taskExecutor);
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJob.count).as("DummyJob should have been executed at least once.").isGreaterThan(0);
		assertThat(taskExecutor.count).isEqualTo(DummyJob.count);

		bean.destroy();
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void jobDetailWithRunnableInsteadOfJob() {
		JobDetailImpl jobDetail = new JobDetailImpl();
		assertThatIllegalArgumentException().isThrownBy(() ->
				jobDetail.setJobClass((Class) DummyRunnable.class));
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerWithQuartzJobBean() throws Exception {
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
		trigger.setRepeatInterval(100);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTriggers(trigger.getObject());
		bean.setJobDetails(jobDetail);
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJobBean.param).isEqualTo(10);
		assertThat(DummyJobBean.count).isGreaterThan(0);

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerWithSpringBeanJobFactory() throws Exception {
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
		trigger.setRepeatInterval(100);
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
		assertThat(DummyJob.count).as("DummyJob should have been executed at least once.").isGreaterThan(0);

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerWithSpringBeanJobFactoryAndParamMismatchNotIgnored() throws Exception {
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
		trigger.setRepeatInterval(100);
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
		assertThat(DummyJob.count).isEqualTo(0);

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerWithSpringBeanJobFactoryAndQuartzJobBean() throws Exception {
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
		trigger.setRepeatInterval(100);
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
		assertThat(DummyJobBean.count).isGreaterThan(0);

		bean.destroy();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerWithSpringBeanJobFactoryAndJobSchedulingData() throws Exception {
		DummyJob.param = 0;
		DummyJob.count = 0;

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setJobSchedulingDataLocation("org/springframework/scheduling/quartz/job-scheduling-data.xml");
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertThat(DummyJob.param).isEqualTo(10);
		assertThat(DummyJob.count).as("DummyJob should have been executed at least once.").isGreaterThan(0);

		bean.destroy();
	}

	@Test  // SPR-772
	void multipleSchedulers() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("multipleSchedulers.xml")) {
			Scheduler scheduler1 = (Scheduler) ctx.getBean("scheduler1");
			Scheduler scheduler2 = (Scheduler) ctx.getBean("scheduler2");
			assertThat(scheduler2).isNotSameAs(scheduler1);
			assertThat(scheduler1.getSchedulerName()).isEqualTo("quartz1");
			assertThat(scheduler2.getSchedulerName()).isEqualTo("quartz2");
		}
	}

	@Test  // SPR-16884
	void multipleSchedulersWithQuartzProperties() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("multipleSchedulersWithQuartzProperties.xml")) {
			Scheduler scheduler1 = (Scheduler) ctx.getBean("scheduler1");
			Scheduler scheduler2 = (Scheduler) ctx.getBean("scheduler2");
			assertThat(scheduler2).isNotSameAs(scheduler1);
			assertThat(scheduler1.getSchedulerName()).isEqualTo("quartz1");
			assertThat(scheduler2.getSchedulerName()).isEqualTo("quartz2");
		}
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void twoAnonymousMethodInvokingJobDetailFactoryBeans() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("multipleAnonymousMethodInvokingJobDetailFB.xml")) {
			QuartzTestBean exportService = (QuartzTestBean) ctx.getBean("exportService");
			QuartzTestBean importService = (QuartzTestBean) ctx.getBean("importService");

			Thread.sleep(400);

			assertThat(exportService.getImportCount()).as("doImport called exportService").isEqualTo(0);
			assertThat(exportService.getExportCount()).as("doExport not called on exportService").isEqualTo(2);
			assertThat(importService.getImportCount()).as("doImport not called on importService").isEqualTo(2);
			assertThat(importService.getExportCount()).as("doExport called on importService").isEqualTo(0);
		}
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void schedulerAccessorBean() throws Exception {
		try (ClassPathXmlApplicationContext ctx = context("schedulerAccessorBean.xml")) {
			QuartzTestBean exportService = (QuartzTestBean) ctx.getBean("exportService");
			QuartzTestBean importService = (QuartzTestBean) ctx.getBean("importService");

			Thread.sleep(400);

			assertThat(exportService.getImportCount()).as("doImport called exportService").isEqualTo(0);
			assertThat(exportService.getExportCount()).as("doExport not called on exportService").isEqualTo(2);
			assertThat(importService.getImportCount()).as("doImport not called on importService").isEqualTo(2);
			assertThat(importService.getExportCount()).as("doExport called on importService").isEqualTo(0);
		}
	}

	@Test
	void schedulerAutoStartsOnContextRefreshedEventByDefault() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("scheduler", new RootBeanDefinition(SchedulerFactoryBean.class));
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertThat(bean.isStarted()).isFalse();
		context.refresh();
		assertThat(bean.isStarted()).isTrue();
	}

	@Test
	void schedulerAutoStartupFalse() throws Exception {
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
	void schedulerRepositoryExposure() {
		try (ClassPathXmlApplicationContext ctx = context("schedulerRepositoryExposure.xml")) {
			assertThat(ctx.getBean("scheduler")).isSameAs(SchedulerRepository.getInstance().lookup("myScheduler"));
		}
	}

	/**
	 * SPR-6038: detect HSQL and stop illegal locks being taken.
	 * TODO: Against Quartz 2.2, this test's job doesn't actually execute anymore...
	 */
	@Test
	void schedulerWithHsqlDataSource() {
		DummyJob.param = 0;
		DummyJob.count = 0;

		try (ClassPathXmlApplicationContext ctx = context("databasePersistence.xml")) {
			JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
			assertThat(jdbcTemplate.queryForList("SELECT * FROM qrtz_triggers").isEmpty()).as("No triggers were persisted").isFalse();
		}
	}

	@Test
	void schedulerFactoryBeanWithCustomJobStore() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();

		String dbName = "mydb";
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setName(dbName).build();

		Properties properties = new Properties();
		properties.setProperty("org.quartz.jobStore.class", JobStoreTX.class.getName());
		properties.setProperty("org.quartz.jobStore.dataSource", dbName);

		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class)
				.addPropertyValue("autoStartup", false)
				.addPropertyValue("dataSource", database)
				.addPropertyValue("quartzProperties", properties)
				.getBeanDefinition();
		context.registerBeanDefinition("scheduler", beanDefinition);

		Scheduler scheduler = context.getBean(Scheduler.class);

		assertThat(scheduler.getMetaData().getJobStoreClass()).isEqualTo(JobStoreTX.class);
	}

	private ClassPathXmlApplicationContext context(String path) {
		return new ClassPathXmlApplicationContext(path, getClass());
	}


	private static class CountingTaskExecutor implements TaskExecutor {

		private int count;

		@Override
		public void execute(Runnable task) {
			this.count++;
			task.run();
		}
	}


	private static class DummyJob implements Job {

		private static int param;

		private static int count;

		@SuppressWarnings("unused")
		// Must be public
		public void setParam(int value) {
			if (param > 0) {
				throw new IllegalStateException("Param already set");
			}
			param = value;
		}

		@Override
		public synchronized void execute(JobExecutionContext jobExecutionContext) {
			count++;
		}
	}


	private static class DummyJobBean extends QuartzJobBean {

		private static int param;

		private static int count;

		@SuppressWarnings("unused")
		public void setParam(int value) {
			if (param > 0) {
				throw new IllegalStateException("Param already set");
			}
			param = value;
		}

		@Override
		protected synchronized void executeInternal(JobExecutionContext jobExecutionContext) {
			count++;
		}
	}


	private static class DummyRunnable implements Runnable {

		@Override
		public void run() {
			/* no-op */
		}
	}

}
