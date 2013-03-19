/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Ignore;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SchedulerListener;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.impl.SchedulerRepository;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.context.TestMethodInvokingTask;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Dave Syer
 * @author Mark Fisher
 * @since 20.02.2004
 */
public class QuartzSupportTests {

	@Test
	public void testSchedulerFactoryBean() throws Exception {
		doTestSchedulerFactoryBean(false, false);
	}

	@Test
	public void testSchedulerFactoryBeanWithExplicitJobDetail() throws Exception {
		doTestSchedulerFactoryBean(true, false);
	}

	@Test
	public void testSchedulerFactoryBeanWithPrototypeJob() throws Exception {
		doTestSchedulerFactoryBean(false, true);
	}

	private void doTestSchedulerFactoryBean(boolean explicitJobDetail, boolean prototypeJob) throws Exception {
		TestBean tb = new TestBean("tb", 99);
		JobDetailBean jobDetail0 = new JobDetailBean();
		jobDetail0.setJobClass(Job.class);
		jobDetail0.setBeanName("myJob0");
		Map jobData = new HashMap();
		jobData.put("testBean", tb);
		jobDetail0.setJobDataAsMap(jobData);
		jobDetail0.afterPropertiesSet();
		assertEquals(tb, jobDetail0.getJobDataMap().get("testBean"));

		CronTriggerBean trigger0 = new CronTriggerBean();
		trigger0.setBeanName("myTrigger0");
		trigger0.setJobDetail(jobDetail0);
		trigger0.setCronExpression("0/1 * * * * ?");
		trigger0.afterPropertiesSet();

		TestMethodInvokingTask task1 = new TestMethodInvokingTask();
		MethodInvokingJobDetailFactoryBean mijdfb = new MethodInvokingJobDetailFactoryBean();
		mijdfb.setBeanName("myJob1");
		if (prototypeJob) {
			StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
			beanFactory.addBean("task", task1);
			mijdfb.setTargetBeanName("task");
			mijdfb.setBeanFactory(beanFactory);
		}
		else {
			mijdfb.setTargetObject(task1);
		}
		mijdfb.setTargetMethod("doSomething");
		mijdfb.afterPropertiesSet();
		JobDetail jobDetail1 = mijdfb.getObject();

		SimpleTriggerBean trigger1 = new SimpleTriggerBean();
		trigger1.setBeanName("myTrigger1");
		trigger1.setJobDetail(jobDetail1);
		trigger1.setStartDelay(0);
		trigger1.setRepeatInterval(20);
		trigger1.afterPropertiesSet();

		final Scheduler scheduler = mock(Scheduler.class);
		given(scheduler.getContext()).willReturn(new SchedulerContext());
		given(scheduler.scheduleJob(trigger0)).willReturn(new Date());
		given(scheduler.scheduleJob(trigger1)).willReturn(new Date());

		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean() {
			@Override
			protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) {
				return scheduler;
			}
		};
		schedulerFactoryBean.setJobFactory(null);
		Map schedulerContext = new HashMap();
		schedulerContext.put("otherTestBean", tb);
		schedulerFactoryBean.setSchedulerContextAsMap(schedulerContext);
		if (explicitJobDetail) {
			schedulerFactoryBean.setJobDetails(new JobDetail[] {jobDetail0});
		}
		schedulerFactoryBean.setTriggers(new Trigger[] {trigger0, trigger1});
		try {
			schedulerFactoryBean.afterPropertiesSet();
			schedulerFactoryBean.start();
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).getJobDetail("myJob0", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getJobDetail("myJob1", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getTrigger("myTrigger0", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getTrigger("myTrigger1", Scheduler.DEFAULT_GROUP);
		verify(scheduler).addJob(jobDetail0, true);
		verify(scheduler).addJob(jobDetail1, true);
		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Test
	public void testSchedulerFactoryBeanWithExistingJobs() throws Exception {
		doTestSchedulerFactoryBeanWithExistingJobs(false);
	}

	@Test
	public void testSchedulerFactoryBeanWithOverwriteExistingJobs() throws Exception {
		doTestSchedulerFactoryBeanWithExistingJobs(true);
	}

	private void doTestSchedulerFactoryBeanWithExistingJobs(boolean overwrite) throws Exception {
		TestBean tb = new TestBean("tb", 99);
		JobDetailBean jobDetail0 = new JobDetailBean();
		jobDetail0.setJobClass(Job.class);
		jobDetail0.setBeanName("myJob0");
		Map jobData = new HashMap();
		jobData.put("testBean", tb);
		jobDetail0.setJobDataAsMap(jobData);
		jobDetail0.afterPropertiesSet();
		assertEquals(tb, jobDetail0.getJobDataMap().get("testBean"));

		CronTriggerBean trigger0 = new CronTriggerBean();
		trigger0.setBeanName("myTrigger0");
		trigger0.setJobDetail(jobDetail0);
		trigger0.setCronExpression("0/1 * * * * ?");
		trigger0.afterPropertiesSet();

		TestMethodInvokingTask task1 = new TestMethodInvokingTask();
		MethodInvokingJobDetailFactoryBean mijdfb = new MethodInvokingJobDetailFactoryBean();
		mijdfb.setBeanName("myJob1");
		mijdfb.setTargetObject(task1);
		mijdfb.setTargetMethod("doSomething");
		mijdfb.afterPropertiesSet();
		JobDetail jobDetail1 = mijdfb.getObject();

		SimpleTriggerBean trigger1 = new SimpleTriggerBean();
		trigger1.setBeanName("myTrigger1");
		trigger1.setJobDetail(jobDetail1);
		trigger1.setStartDelay(0);
		trigger1.setRepeatInterval(20);
		trigger1.afterPropertiesSet();

		final Scheduler scheduler = mock(Scheduler.class);
		given(scheduler.getContext()).willReturn(new SchedulerContext());
		given(scheduler.rescheduleJob("myTrigger1", Scheduler.DEFAULT_GROUP, trigger1)).willReturn(new Date());
		given(scheduler.getTrigger("myTrigger1", Scheduler.DEFAULT_GROUP)).willReturn(new SimpleTrigger());
		given(scheduler.scheduleJob(trigger0)).willReturn(new Date());

		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean() {
			@Override
			protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) {
				return scheduler;
			}
		};
		schedulerFactoryBean.setJobFactory(null);
		Map schedulerContext = new HashMap();
		schedulerContext.put("otherTestBean", tb);
		schedulerFactoryBean.setSchedulerContextAsMap(schedulerContext);
		schedulerFactoryBean.setTriggers(new Trigger[] {trigger0, trigger1});
		if (overwrite) {
			schedulerFactoryBean.setOverwriteExistingJobs(true);
		}
		try {
			schedulerFactoryBean.afterPropertiesSet();
			schedulerFactoryBean.start();
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).getTrigger("myTrigger0", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getTrigger("myTrigger1", Scheduler.DEFAULT_GROUP);
		if (overwrite) {
			verify(scheduler).addJob(jobDetail1, true);
			verify(scheduler).rescheduleJob("myTrigger1", Scheduler.DEFAULT_GROUP, trigger1);
		}
		else {
			verify(scheduler).getJobDetail("myJob0", Scheduler.DEFAULT_GROUP);
		}
		verify(scheduler).addJob(jobDetail0, true);
		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Test
	public void testSchedulerFactoryBeanWithExistingJobsAndRaceCondition() throws Exception {
		doTestSchedulerFactoryBeanWithExistingJobsAndRaceCondition(false);
	}

	@Test
	public void testSchedulerFactoryBeanWithOverwriteExistingJobsAndRaceCondition() throws Exception {
		doTestSchedulerFactoryBeanWithExistingJobsAndRaceCondition(true);
	}

	private void doTestSchedulerFactoryBeanWithExistingJobsAndRaceCondition(boolean overwrite) throws Exception {
		TestBean tb = new TestBean("tb", 99);
		JobDetailBean jobDetail0 = new JobDetailBean();
		jobDetail0.setJobClass(Job.class);
		jobDetail0.setBeanName("myJob0");
		Map jobData = new HashMap();
		jobData.put("testBean", tb);
		jobDetail0.setJobDataAsMap(jobData);
		jobDetail0.afterPropertiesSet();
		assertEquals(tb, jobDetail0.getJobDataMap().get("testBean"));

		CronTriggerBean trigger0 = new CronTriggerBean();
		trigger0.setBeanName("myTrigger0");
		trigger0.setJobDetail(jobDetail0);
		trigger0.setCronExpression("0/1 * * * * ?");
		trigger0.afterPropertiesSet();

		TestMethodInvokingTask task1 = new TestMethodInvokingTask();
		MethodInvokingJobDetailFactoryBean mijdfb = new MethodInvokingJobDetailFactoryBean();
		mijdfb.setBeanName("myJob1");
		mijdfb.setTargetObject(task1);
		mijdfb.setTargetMethod("doSomething");
		mijdfb.afterPropertiesSet();
		JobDetail jobDetail1 = mijdfb.getObject();

		SimpleTriggerBean trigger1 = new SimpleTriggerBean();
		trigger1.setBeanName("myTrigger1");
		trigger1.setJobDetail(jobDetail1);
		trigger1.setStartDelay(0);
		trigger1.setRepeatInterval(20);
		trigger1.afterPropertiesSet();

		final Scheduler scheduler = mock(Scheduler.class);
		given(scheduler.getContext()).willReturn(new SchedulerContext());
		given(scheduler.getTrigger("myTrigger1", Scheduler.DEFAULT_GROUP)).willReturn(new SimpleTrigger());
		if (overwrite) {
			given(scheduler.rescheduleJob("myTrigger1", Scheduler.DEFAULT_GROUP, trigger1)).willReturn(new Date());
		}
		given(scheduler.scheduleJob(trigger0)).willThrow(new ObjectAlreadyExistsException(""));
		if (overwrite) {
			given(scheduler.rescheduleJob("myTrigger0", Scheduler.DEFAULT_GROUP, trigger0)).willReturn(new Date());
		}

		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean() {
			@Override
			protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) {
				return scheduler;
			}
		};
		schedulerFactoryBean.setJobFactory(null);
		Map schedulerContext = new HashMap();
		schedulerContext.put("otherTestBean", tb);
		schedulerFactoryBean.setSchedulerContextAsMap(schedulerContext);
		schedulerFactoryBean.setTriggers(new Trigger[] {trigger0, trigger1});
		if (overwrite) {
			schedulerFactoryBean.setOverwriteExistingJobs(true);
		}
		try {
			schedulerFactoryBean.afterPropertiesSet();
			schedulerFactoryBean.start();
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).getTrigger("myTrigger0", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getTrigger("myTrigger1", Scheduler.DEFAULT_GROUP);
		if (overwrite) {
			verify(scheduler).addJob(jobDetail1, true);
			verify(scheduler).rescheduleJob("myTrigger1", Scheduler.DEFAULT_GROUP, trigger1);
		}
		else {
			verify(scheduler).getJobDetail("myJob0", Scheduler.DEFAULT_GROUP);
		}
		verify(scheduler).addJob(jobDetail0, true);
		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Test
	public void testSchedulerFactoryBeanWithListeners() throws Exception {
		JobFactory jobFactory = new AdaptableJobFactory();

		final Scheduler scheduler = mock(Scheduler.class);

		SchedulerListener schedulerListener = new TestSchedulerListener();
		JobListener globalJobListener = new TestJobListener();
		JobListener jobListener = new TestJobListener();
		TriggerListener globalTriggerListener = new TestTriggerListener();
		TriggerListener triggerListener = new TestTriggerListener();

		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean() {
			@Override
			protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) {
				return scheduler;
			}
		};
		schedulerFactoryBean.setJobFactory(jobFactory);
		schedulerFactoryBean.setSchedulerListeners(new SchedulerListener[] {schedulerListener});
		schedulerFactoryBean.setGlobalJobListeners(new JobListener[] {globalJobListener});
		schedulerFactoryBean.setJobListeners(new JobListener[] {jobListener});
		schedulerFactoryBean.setGlobalTriggerListeners(new TriggerListener[] {globalTriggerListener});
		schedulerFactoryBean.setTriggerListeners(new TriggerListener[] {triggerListener});
		try {
			schedulerFactoryBean.afterPropertiesSet();
			schedulerFactoryBean.start();
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).setJobFactory(jobFactory);
		verify(scheduler).addSchedulerListener(schedulerListener);
		verify(scheduler).addGlobalJobListener(globalJobListener);
		verify(scheduler).addJobListener(jobListener);
		verify(scheduler).addGlobalTriggerListener(globalTriggerListener);
		verify(scheduler).addTriggerListener(triggerListener);
		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Ignore @Test
	public void testMethodInvocationWithConcurrency() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		methodInvokingConcurrency(true);
	}

	// We can't test both since Quartz somehow seems to keep things in memory
	// enable both and one of them will fail (order doesn't matter).
	@Ignore @Test
	public void testMethodInvocationWithoutConcurrency() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		methodInvokingConcurrency(false);
	}

	private void methodInvokingConcurrency(boolean concurrent) throws Exception {
		// Test the concurrency flag.
		// Method invoking job with two triggers.
		// If the concurrent flag is false, the triggers are NOT allowed
		// to interfere with each other.

		TestMethodInvokingTask task1 = new TestMethodInvokingTask();
		MethodInvokingJobDetailFactoryBean mijdfb = new MethodInvokingJobDetailFactoryBean();
		// set the concurrency flag!
		mijdfb.setConcurrent(concurrent);
		mijdfb.setBeanName("myJob1");
		mijdfb.setTargetObject(task1);
		mijdfb.setTargetMethod("doWait");
		mijdfb.afterPropertiesSet();
		JobDetail jobDetail1 = mijdfb.getObject();

		SimpleTriggerBean trigger0 = new SimpleTriggerBean();
		trigger0.setBeanName("myTrigger1");
		trigger0.setJobDetail(jobDetail1);
		trigger0.setStartDelay(0);
		trigger0.setRepeatInterval(1);
		trigger0.setRepeatCount(1);
		trigger0.afterPropertiesSet();

		SimpleTriggerBean trigger1 = new SimpleTriggerBean();
		trigger1.setBeanName("myTrigger1");
		trigger1.setJobDetail(jobDetail1);
		trigger1.setStartDelay(1000L);
		trigger1.setRepeatInterval(1);
		trigger1.setRepeatCount(1);
		trigger1.afterPropertiesSet();

		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
		schedulerFactoryBean.setJobDetails(new JobDetail[] {jobDetail1});
		schedulerFactoryBean.setTriggers(new Trigger[] {trigger1, trigger0});
		schedulerFactoryBean.afterPropertiesSet();

		// ok scheduler is set up... let's wait for like 4 seconds
		try {
			Thread.sleep(4000);
		}
		catch (InterruptedException ex) {
			// fall through
		}

		if (concurrent) {
			assertEquals(2, task1.counter);
			task1.stop();
			// we're done, both jobs have ran, let's call it a day
			return;
		}
		else {
			assertEquals(1, task1.counter);
			task1.stop();
			// we need to check whether or not the test succeed with non-concurrent jobs
		}

		try {
			Thread.sleep(4000);
		}
		catch (InterruptedException ex) {
			// fall through
		}

		task1.stop();
		assertEquals(2, task1.counter);

		// Although we're destroying the scheduler, it does seem to keep things in memory:
		// When executing both tests (concurrent and non-concurrent), the second test always
		// fails.
		schedulerFactoryBean.destroy();
	}

	@Test
	public void testSchedulerFactoryBeanWithPlainQuartzObjects() throws Exception {
		JobFactory jobFactory = new AdaptableJobFactory();

		TestBean tb = new TestBean("tb", 99);
		JobDetail jobDetail0 = new JobDetail();
		jobDetail0.setJobClass(Job.class);
		jobDetail0.setName("myJob0");
		jobDetail0.setGroup(Scheduler.DEFAULT_GROUP);
		jobDetail0.getJobDataMap().put("testBean", tb);
		assertEquals(tb, jobDetail0.getJobDataMap().get("testBean"));

		CronTrigger trigger0 = new CronTrigger();
		trigger0.setName("myTrigger0");
		trigger0.setGroup(Scheduler.DEFAULT_GROUP);
		trigger0.setJobName("myJob0");
		trigger0.setJobGroup(Scheduler.DEFAULT_GROUP);
		trigger0.setStartTime(new Date());
		trigger0.setCronExpression("0/1 * * * * ?");

		TestMethodInvokingTask task1 = new TestMethodInvokingTask();
		MethodInvokingJobDetailFactoryBean mijdfb = new MethodInvokingJobDetailFactoryBean();
		mijdfb.setName("myJob1");
		mijdfb.setGroup(Scheduler.DEFAULT_GROUP);
		mijdfb.setTargetObject(task1);
		mijdfb.setTargetMethod("doSomething");
		mijdfb.afterPropertiesSet();
		JobDetail jobDetail1 = mijdfb.getObject();

		SimpleTrigger trigger1 = new SimpleTrigger();
		trigger1.setName("myTrigger1");
		trigger1.setGroup(Scheduler.DEFAULT_GROUP);
		trigger1.setJobName("myJob1");
		trigger1.setJobGroup(Scheduler.DEFAULT_GROUP);
		trigger1.setStartTime(new Date());
		trigger1.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
		trigger1.setRepeatInterval(20);

		final Scheduler scheduler = mock(Scheduler.class);
		given(scheduler.scheduleJob(trigger0)).willReturn(new Date());
		given(scheduler.scheduleJob(trigger1)).willReturn(new Date());

		SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean() {
			@Override
			protected Scheduler createScheduler(SchedulerFactory schedulerFactory, String schedulerName) {
				return scheduler;
			}
		};
		schedulerFactoryBean.setJobFactory(jobFactory);
		schedulerFactoryBean.setJobDetails(new JobDetail[] {jobDetail0, jobDetail1});
		schedulerFactoryBean.setTriggers(new Trigger[] {trigger0, trigger1});
		try {
			schedulerFactoryBean.afterPropertiesSet();
			schedulerFactoryBean.start();
		}
		finally {
			schedulerFactoryBean.destroy();
		}

		verify(scheduler).setJobFactory(jobFactory);
		verify(scheduler).getJobDetail("myJob0", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getJobDetail("myJob1", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getTrigger("myTrigger0", Scheduler.DEFAULT_GROUP);
		verify(scheduler).getTrigger("myTrigger1", Scheduler.DEFAULT_GROUP);
		verify(scheduler).addJob(jobDetail0, true);
		verify(scheduler).addJob(jobDetail1, true);
		verify(scheduler).scheduleJob(trigger0);
		verify(scheduler).scheduleJob(trigger1);
		verify(scheduler).start();
		verify(scheduler).shutdown(false);
	}

	@Test
	public void testSchedulerFactoryBeanWithApplicationContext() throws Exception {
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
		Map schedulerContextMap = new HashMap();
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
	public void testJobDetailBeanWithApplicationContext() throws Exception {
		TestBean tb = new TestBean("tb", 99);
		StaticApplicationContext ac = new StaticApplicationContext();

		JobDetailBean jobDetail = new JobDetailBean();
		jobDetail.setJobClass(Job.class);
		jobDetail.setBeanName("myJob0");
		Map jobData = new HashMap();
		jobData.put("testBean", tb);
		jobDetail.setJobDataAsMap(jobData);
		jobDetail.setApplicationContext(ac);
		jobDetail.setApplicationContextJobDataKey("appCtx");
		jobDetail.afterPropertiesSet();

		assertEquals(tb, jobDetail.getJobDataMap().get("testBean"));
		assertEquals(ac, jobDetail.getJobDataMap().get("appCtx"));
	}

	@Test
	public void testMethodInvokingJobDetailFactoryBeanWithListenerNames() throws Exception {
		TestMethodInvokingTask task = new TestMethodInvokingTask();
		MethodInvokingJobDetailFactoryBean mijdfb = new MethodInvokingJobDetailFactoryBean();
		String[] names = new String[] {"test1", "test2"};
		mijdfb.setName("myJob1");
		mijdfb.setGroup(Scheduler.DEFAULT_GROUP);
		mijdfb.setTargetObject(task);
		mijdfb.setTargetMethod("doSomething");
		mijdfb.setJobListenerNames(names);
		mijdfb.afterPropertiesSet();
		JobDetail jobDetail = mijdfb.getObject();
		List result = Arrays.asList(jobDetail.getJobListenerNames());
		assertEquals(Arrays.asList(names), result);
	}

	@Test
	public void testJobDetailBeanWithListenerNames() {
		JobDetailBean jobDetail = new JobDetailBean();
		String[] names = new String[] {"test1", "test2"};
		jobDetail.setJobListenerNames(names);
		List result = Arrays.asList(jobDetail.getJobListenerNames());
		assertEquals(Arrays.asList(names), result);
	}

	@Test
	public void testCronTriggerBeanWithListenerNames() {
		CronTriggerBean trigger = new CronTriggerBean();
		String[] names = new String[] {"test1", "test2"};
		trigger.setTriggerListenerNames(names);
		List result = Arrays.asList(trigger.getTriggerListenerNames());
		assertEquals(Arrays.asList(names), result);
	}

	@Test
	public void testSimpleTriggerBeanWithListenerNames() {
		SimpleTriggerBean trigger = new SimpleTriggerBean();
		String[] names = new String[] {"test1", "test2"};
		trigger.setTriggerListenerNames(names);
		List result = Arrays.asList(trigger.getTriggerListenerNames());
		assertEquals(Arrays.asList(names), result);
	}

	@Test
	public void testSchedulerWithTaskExecutor() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		CountingTaskExecutor taskExecutor = new CountingTaskExecutor();
		DummyJob.count = 0;

		JobDetail jobDetail = new JobDetail();
		jobDetail.setJobClass(DummyJob.class);
		jobDetail.setName("myJob");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTaskExecutor(taskExecutor);
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertTrue(DummyJob.count > 0);
		assertEquals(DummyJob.count, taskExecutor.count);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithRunnable() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		DummyRunnable.count = 0;

		JobDetail jobDetail = new JobDetailBean();
		jobDetail.setJobClass(DummyRunnable.class);
		jobDetail.setName("myJob");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertTrue(DummyRunnable.count > 0);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithQuartzJobBean() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		DummyJob.param = 0;
		DummyJob.count = 0;

		JobDetail jobDetail = new JobDetail();
		jobDetail.setJobClass(DummyJobBean.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertEquals(10, DummyJobBean.param);
		assertTrue(DummyJobBean.count > 0);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithSpringBeanJobFactory() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		DummyJob.param = 0;
		DummyJob.count = 0;

		JobDetail jobDetail = new JobDetail();
		jobDetail.setJobClass(DummyJob.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");
		jobDetail.getJobDataMap().put("ignoredParam", "10");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertEquals(10, DummyJob.param);
		assertTrue(DummyJob.count > 0);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithSpringBeanJobFactoryAndParamMismatchNotIgnored() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		DummyJob.param = 0;
		DummyJob.count = 0;

		JobDetail jobDetail = new JobDetail();
		jobDetail.setJobClass(DummyJob.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("para", "10");
		jobDetail.getJobDataMap().put("ignoredParam", "10");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
		jobFactory.setIgnoredUnknownProperties(new String[] {"ignoredParam"});
		bean.setJobFactory(jobFactory);
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();

		Thread.sleep(500);
		assertEquals(0, DummyJob.param);
		assertTrue(DummyJob.count == 0);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithSpringBeanJobFactoryAndRunnable() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		DummyRunnable.param = 0;
		DummyRunnable.count = 0;

		JobDetail jobDetail = new JobDetailBean();
		jobDetail.setJobClass(DummyRunnable.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertEquals(10, DummyRunnable.param);
		assertTrue(DummyRunnable.count > 0);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithSpringBeanJobFactoryAndQuartzJobBean() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		DummyJobBean.param = 0;
		DummyJobBean.count = 0;

		JobDetail jobDetail = new JobDetail();
		jobDetail.setJobClass(DummyJobBean.class);
		jobDetail.setName("myJob");
		jobDetail.getJobDataMap().put("param", "10");

		SimpleTriggerBean trigger = new SimpleTriggerBean();
		trigger.setName("myTrigger");
		trigger.setJobDetail(jobDetail);
		trigger.setStartDelay(1);
		trigger.setRepeatInterval(500);
		trigger.setRepeatCount(1);
		trigger.afterPropertiesSet();

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setTriggers(new Trigger[] {trigger});
		bean.setJobDetails(new JobDetail[] {jobDetail});
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertEquals(10, DummyJobBean.param);
		assertTrue(DummyJobBean.count > 0);

		bean.destroy();
	}

	@Test
	public void testSchedulerWithSpringBeanJobFactoryAndJobSchedulingData() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		DummyJob.param = 0;
		DummyJob.count = 0;

		SchedulerFactoryBean bean = new SchedulerFactoryBean();
		bean.setJobFactory(new SpringBeanJobFactory());
		bean.setJobSchedulingDataLocation("org/springframework/scheduling/quartz/job-scheduling-data.xml");
		bean.setResourceLoader(new FileSystemResourceLoader());
		bean.afterPropertiesSet();
		bean.start();

		Thread.sleep(500);
		assertEquals(10, DummyJob.param);
		assertTrue(DummyJob.count > 0);

		bean.destroy();
	}

	/**
	 * Tests the creation of multiple schedulers (SPR-772)
	 */
	@Test
	public void testMultipleSchedulers() throws Exception {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext("/org/springframework/scheduling/quartz/multipleSchedulers.xml");
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
	public void testWithTwoAnonymousMethodInvokingJobDetailFactoryBeans() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext("/org/springframework/scheduling/quartz/multipleAnonymousMethodInvokingJobDetailFB.xml");
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
	public void testSchedulerAccessorBean() throws InterruptedException {
		Assume.group(TestGroup.PERFORMANCE);
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext("/org/springframework/scheduling/quartz/schedulerAccessorBean.xml");
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
	public void testSchedulerAutoStartsOnContextRefreshedEventByDefault() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		context.registerBeanDefinition("scheduler", new RootBeanDefinition(SchedulerFactoryBean.class));
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertFalse(bean.isStarted());
		context.refresh();
		assertTrue(bean.isStarted());
	}

	@Test
	public void testSchedulerAutoStartupFalse() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(
				SchedulerFactoryBean.class).addPropertyValue("autoStartup", false).getBeanDefinition();
		context.registerBeanDefinition("scheduler", beanDefinition);
		Scheduler bean = context.getBean("scheduler", Scheduler.class);
		assertFalse(bean.isStarted());
		context.refresh();
		assertFalse(bean.isStarted());
	}

	@Test
	public void testSchedulerRepositoryExposure() throws InterruptedException {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext("/org/springframework/scheduling/quartz/schedulerRepositoryExposure.xml");
		assertSame(SchedulerRepository.getInstance().lookup("myScheduler"), ctx.getBean("scheduler"));
		ctx.close();
	}

	// SPR-6038: detect HSQL and stop illegal locks being taken
	@Test
	public void testSchedulerWithHsqlDataSource() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);

		DummyJob.param = 0;
		DummyJob.count = 0;

		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(
				"/org/springframework/scheduling/quartz/databasePersistence.xml");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(ctx.getBean(DataSource.class));
		assertTrue("No triggers were persisted", jdbcTemplate.queryForList("SELECT * FROM qrtz_triggers").size()>0);
		Thread.sleep(3000);
		try {
			// assertEquals(10, DummyJob.param);
			assertTrue(DummyJob.count > 0);
		} finally {
			ctx.close();
		}

	}

	private static class TestSchedulerListener implements SchedulerListener {

		@Override
		public void jobScheduled(Trigger trigger) {
		}

		@Override
		public void jobUnscheduled(String triggerName, String triggerGroup) {
		}

		@Override
		public void triggerFinalized(Trigger trigger) {
		}

		@Override
		public void triggersPaused(String triggerName, String triggerGroup) {
		}

		@Override
		public void triggersResumed(String triggerName, String triggerGroup) {
		}

		@Override
		public void jobsPaused(String jobName, String jobGroup) {
		}

		@Override
		public void jobsResumed(String jobName, String jobGroup) {
		}

		@Override
		public void schedulerError(String msg, SchedulerException cause) {
		}

		@Override
		public void schedulerShutdown() {
		}

		@Override
		public void jobAdded(JobDetail jobDetail) {
		}

		@Override
		public void jobDeleted(String s, String s1) {
		}

		@Override
		public void schedulerInStandbyMode() {
		}

		@Override
		public void schedulerStarted() {
		}

		@Override
		public void schedulerShuttingdown() {
		}
	}


	private static class TestJobListener implements JobListener {

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void jobToBeExecuted(JobExecutionContext context) {
		}

		@Override
		public void jobExecutionVetoed(JobExecutionContext context) {
		}

		@Override
		public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		}
	}


	private static class TestTriggerListener implements TriggerListener {

		@Override
		public String getName() {
			return null;
		}

		@Override
		public void triggerFired(Trigger trigger, JobExecutionContext context) {
		}

		@Override
		public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
			return false;
		}

		@Override
		public void triggerMisfired(Trigger trigger) {
		}

		@Override
		public void triggerComplete(Trigger trigger, JobExecutionContext context, int triggerInstructionCode) {
		}
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

		private static int param;

		private static int count;

		public void setParam(int value) {
			if (param > 0) {
				throw new IllegalStateException("Param already set");
			}
			param = value;
		}

		@Override
		public void run() {
			count++;
		}
	}

}
