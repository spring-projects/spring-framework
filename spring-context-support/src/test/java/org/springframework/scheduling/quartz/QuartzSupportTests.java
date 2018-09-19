/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.quartz;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.Test;
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
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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
			assertEquals(tb, returnedScheduler.getContext().get("testBean"));
			assertEquals(ac, returnedScheduler.getContext().get("appCtx"));
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Test
	public void schedulerWithTaskExecutor() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

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
		assertTrue("DummyJob should have been executed at least once.", DummyJob.count > 0);
		assertEquals(DummyJob.count, taskExecutor.count);

		bean.destroy();
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void jobDetailWithRunnableInsteadOfJob() {
		JobDetailImpl jobDetail = new JobDetailImpl();
		jobDetail.setJobClass((Class) DummyRunnable.class);
	}

	@Test
	public void schedulerWithQuartzJobBean() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

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
		assertEquals(10, DummyJobBean.param);
		assertTrue(DummyJobBean.count > 0);

		bean.destroy();
	}

	@Test
	public void schedulerWithSpringBeanJobFactory() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

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
		assertEquals(10, DummyJob.param);
		assertTrue("DummyJob should have been executed at least once.", DummyJob.count > 0);

		bean.destroy();
	}

	@Test
	public void schedulerWithSpringBeanJobFactoryAndParamMismatchNotIgnored() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

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
		assertEquals(0, DummyJob.param);
		assertTrue(DummyJob.count == 0);

		bean.destroy();
	}

	@Test
	public void schedulerWithSpringBeanJobFactoryAndQuartzJobBean() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
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
		assertEquals(10, DummyJobBean.param);
		assertTrue(DummyJobBean.count > 0);

		bean.destroy();
	}

	@Test
	public void schedulerWithSpringBeanJobFactoryAndJobSchedulingData() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		DummyJob.param = 0;
		DummyJob.count = 0;

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setJobSchedulingDataLocation("org/springframework/scheduling/quartz/job-scheduling-data.xml");
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertEquals(10, DummyJob.param);
		assertTrue("DummyJob should have been executed at least once.", DummyJob.count > 0);

		bean.destroy();
	}

	@Test  // SPR-772
	public void multipleSchedulers() throws Exception {
		ClassPathXmlApplicationContext ctx = context("multipleSchedulers.xml");
		try {
			Scheduler scheduler1 = (Scheduler) ctx.getBean("scheduler1");
			Scheduler scheduler2 = (Scheduler) ctx.getBean("scheduler2");
			assertNotSame(scheduler1, scheduler2);
			assertEquals("quartz1", scheduler1.getSchedulerName());
			assertEquals("quartz2", scheduler2.getSchedulerName());
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void twoAnonymousMethodInvokingJobDetailFactoryBeans() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		ClassPathXmlApplicationContext ctx = context("multipleAnonymousMethodInvokingJobDetailFB.xml");
		Thread.sleep(3000);
		try {
			QuartzTestBean exportService = (QuartzTestBean) ctx.getBean("exportService");
			QuartzTestBean importService = (QuartzTestBean) ctx.getBean("importService");

			assertEquals("doImport called exportService", 0, exportService.getImportCount());
			assertEquals("doExport not called on exportService", 2, exportService.getExportCount());
			assertEquals("doImport not called on importService", 2, importService.getImportCount());
			assertEquals("doExport called on importService", 0, importService.getExportCount());
		}
		finally {
			ctx.close();
		}
	}

	@Test
	public void schedulerAccessorBean() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		ClassPathXmlApplicationContext ctx = context("schedulerAccessorBean.xml");
		Thread.sleep(3000);
		try {
			QuartzTestBean exportService = (QuartzTestBean) ctx.getBean("exportService");
			QuartzTestBean importService = (QuartzTestBean) ctx.getBean("importService");

			assertEquals("doImport called exportService", 0, exportService.getImportCount());
			assertEquals("doExport not called on exportService", 2, exportService.getExportCount());
			assertEquals("doImport not called on importService", 2, importService.getImportCount());
			assertEquals("doExport called on importService", 0, importService.getExportCount());
		}
		finally {
			ctx.close();
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void schedulerAutoStartsOnContextRefreshedEventByDefault() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("scheduler", new RootBeanDefinition(SchedulerFactoryBean.class));
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertFalse(bean.isStarted());
		context.refresh();
		assertTrue(bean.isStarted());
	}

	@Test
	@SuppressWarnings("resource")
	public void schedulerAutoStartupFalse() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(SchedulerFactoryBean.class)
				.addPropertyValue("autoStartup", false).getBeanDefinition();
		context.registerBeanDefinition("scheduler", beanDefinition);
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertFalse(bean.isStarted());
		context.refresh();
		assertFalse(bean.isStarted());
	}

	@Test
	public void schedulerRepositoryExposure() throws Exception {
		ClassPathXmlApplicationContext ctx = context("schedulerRepositoryExposure.xml");
		assertSame(SchedulerRepository.getInstance().lookup("myScheduler"), ctx.getBean("scheduler"));
		ctx.close();
	}

	/**
	 * SPR-6038: detect HSQL and stop illegal locks being taken.
	 * TODO: Against Quartz 2.2, this test's job doesn't actually execute anymore...
	 */
	@Test
	public void schedulerWithHsqlDataSource() throws Exception {
		// Assume.group(TestGroup.PERFORMANCE);

		DummyJob.param = 0;
		DummyJob.count = 0;

		ClassPathXmlApplicationContext ctx = context("databasePersistence.xml");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
		assertFalse("No triggers were persisted", jdbcTemplate.queryForList("SELECT * FROM qrtz_triggers").isEmpty());

		/*
		Thread.sleep(3000);
		try {
			assertTrue("DummyJob should have been executed at least once.", DummyJob.count > 0);
		}
		finally {
			ctx.close();
		}
		*/
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
