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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobListener;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.spi.ClassLoadHelper;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.ReflectionUtils;

/**
 * Common base class for accessing a Quartz Scheduler, i.e. for registering jobs,
 * triggers and listeners on a {@link org.quartz.Scheduler} instance.
 *
 * <p>For concrete usage, check out the {@link SchedulerFactoryBean} and
 * {@link SchedulerAccessorBean} classes.
 *
 * <p>Compatible with Quartz 1.5+ as well as Quartz 2.0-2.2, as of Spring 3.2.
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public abstract class SchedulerAccessor implements ResourceLoaderAware {

	private static Class<?> jobKeyClass;

	private static Class<?> triggerKeyClass;

	static {
		try {
			jobKeyClass = Class.forName("org.quartz.JobKey");
			triggerKeyClass = Class.forName("org.quartz.TriggerKey");
		}
		catch (ClassNotFoundException ex) {
			jobKeyClass = null;
			triggerKeyClass = null;
		}
	}


	protected final Log logger = LogFactory.getLog(getClass());

	private boolean overwriteExistingJobs = false;

	private String[] jobSchedulingDataLocations;

	private List<JobDetail> jobDetails;

	private Map<String, Calendar> calendars;

	private List<Trigger> triggers;

	private SchedulerListener[] schedulerListeners;

	private JobListener[] globalJobListeners;

	private JobListener[] jobListeners;

	private TriggerListener[] globalTriggerListeners;

	private TriggerListener[] triggerListeners;

	private PlatformTransactionManager transactionManager;

	protected ResourceLoader resourceLoader;


	/**
	 * Set whether any jobs defined on this SchedulerFactoryBean should overwrite
	 * existing job definitions. Default is "false", to not overwrite already
	 * registered jobs that have been read in from a persistent job store.
	 */
	public void setOverwriteExistingJobs(boolean overwriteExistingJobs) {
		this.overwriteExistingJobs = overwriteExistingJobs;
	}

	/**
	 * Set the location of a Quartz job definition XML file that follows the
	 * "job_scheduling_data_1_5" XSD or better. Can be specified to automatically
	 * register jobs that are defined in such a file, possibly in addition
	 * to jobs defined directly on this SchedulerFactoryBean.
	 * @see org.quartz.xml.XmlSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocation(String jobSchedulingDataLocation) {
		this.jobSchedulingDataLocations = new String[] {jobSchedulingDataLocation};
	}

	/**
	 * Set the locations of Quartz job definition XML files that follow the
	 * "job_scheduling_data_1_5" XSD or better. Can be specified to automatically
	 * register jobs that are defined in such files, possibly in addition
	 * to jobs defined directly on this SchedulerFactoryBean.
	 * @see org.quartz.xml.XmlSchedulingDataProcessor
	 */
	public void setJobSchedulingDataLocations(String... jobSchedulingDataLocations) {
		this.jobSchedulingDataLocations = jobSchedulingDataLocations;
	}

	/**
	 * Register a list of JobDetail objects with the Scheduler that
	 * this FactoryBean creates, to be referenced by Triggers.
	 * <p>This is not necessary when a Trigger determines the JobDetail
	 * itself: In this case, the JobDetail will be implicitly registered
	 * in combination with the Trigger.
	 * @see #setTriggers
	 * @see org.quartz.JobDetail
	 * @see JobDetailBean
	 * @see JobDetailAwareTrigger
	 */
	public void setJobDetails(JobDetail... jobDetails) {
		// Use modifiable ArrayList here, to allow for further adding of
		// JobDetail objects during autodetection of JobDetailAwareTriggers.
		this.jobDetails = new ArrayList<JobDetail>(Arrays.asList(jobDetails));
	}

	/**
	 * Register a list of Quartz Calendar objects with the Scheduler
	 * that this FactoryBean creates, to be referenced by Triggers.
	 * @param calendars Map with calendar names as keys as Calendar
	 * objects as values
	 * @see org.quartz.Calendar
	 */
	public void setCalendars(Map<String, Calendar> calendars) {
		this.calendars = calendars;
	}

	/**
	 * Register a list of Trigger objects with the Scheduler that
	 * this FactoryBean creates.
	 * <p>If the Trigger determines the corresponding JobDetail itself,
	 * the job will be automatically registered with the Scheduler.
	 * Else, the respective JobDetail needs to be registered via the
	 * "jobDetails" property of this FactoryBean.
	 * @see #setJobDetails
	 * @see org.quartz.JobDetail
	 * @see JobDetailAwareTrigger
	 * @see CronTriggerBean
	 * @see SimpleTriggerBean
	 */
	public void setTriggers(Trigger... triggers) {
		this.triggers = Arrays.asList(triggers);
	}


	/**
	 * Specify Quartz SchedulerListeners to be registered with the Scheduler.
	 */
	public void setSchedulerListeners(SchedulerListener... schedulerListeners) {
		this.schedulerListeners = schedulerListeners;
	}

	/**
	 * Specify global Quartz JobListeners to be registered with the Scheduler.
	 * Such JobListeners will apply to all Jobs in the Scheduler.
	 */
	public void setGlobalJobListeners(JobListener... globalJobListeners) {
		this.globalJobListeners = globalJobListeners;
	}

	/**
	 * Specify named Quartz JobListeners to be registered with the Scheduler.
	 * Such JobListeners will only apply to Jobs that explicitly activate
	 * them via their name.
	 * <p>Note that non-global JobListeners are not supported on Quartz 2.x -
	 * manually register a Matcher against the Quartz ListenerManager instead.
	 * @see org.quartz.JobListener#getName
	 * @see JobDetailBean#setJobListenerNames
	 */
	public void setJobListeners(JobListener... jobListeners) {
		this.jobListeners = jobListeners;
	}

	/**
	 * Specify global Quartz TriggerListeners to be registered with the Scheduler.
	 * Such TriggerListeners will apply to all Triggers in the Scheduler.
	 */
	public void setGlobalTriggerListeners(TriggerListener... globalTriggerListeners) {
		this.globalTriggerListeners = globalTriggerListeners;
	}

	/**
	 * Specify named Quartz TriggerListeners to be registered with the Scheduler.
	 * Such TriggerListeners will only apply to Triggers that explicitly activate
	 * them via their name.
	 * <p>Note that non-global TriggerListeners are not supported on Quartz 2.x -
	 * manually register a Matcher against the Quartz ListenerManager instead.
	 * @see org.quartz.TriggerListener#getName
	 * @see CronTriggerBean#setTriggerListenerNames
	 * @see SimpleTriggerBean#setTriggerListenerNames
	 */
	public void setTriggerListeners(TriggerListener... triggerListeners) {
		this.triggerListeners = triggerListeners;
	}


	/**
	 * Set the transaction manager to be used for registering jobs and triggers
	 * that are defined by this SchedulerFactoryBean. Default is none; setting
	 * this only makes sense when specifying a DataSource for the Scheduler.
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	/**
	 * Register jobs and triggers (within a transaction, if possible).
	 */
	protected void registerJobsAndTriggers() throws SchedulerException {
		TransactionStatus transactionStatus = null;
		if (this.transactionManager != null) {
			transactionStatus = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
		}

		try {
			if (this.jobSchedulingDataLocations != null) {
				ClassLoadHelper clh = new ResourceLoaderClassLoadHelper(this.resourceLoader);
				clh.initialize();
				try {
					// Quartz 1.8 or higher?
					Class dataProcessorClass = getClass().getClassLoader().loadClass("org.quartz.xml.XMLSchedulingDataProcessor");
					logger.debug("Using Quartz 1.8 XMLSchedulingDataProcessor");
					Object dataProcessor = dataProcessorClass.getConstructor(ClassLoadHelper.class).newInstance(clh);
					Method processFileAndScheduleJobs = dataProcessorClass.getMethod("processFileAndScheduleJobs", String.class, Scheduler.class);
					for (String location : this.jobSchedulingDataLocations) {
						processFileAndScheduleJobs.invoke(dataProcessor, location, getScheduler());
					}
				}
				catch (ClassNotFoundException ex) {
					// Quartz 1.6
					Class dataProcessorClass = getClass().getClassLoader().loadClass("org.quartz.xml.JobSchedulingDataProcessor");
					logger.debug("Using Quartz 1.6 JobSchedulingDataProcessor");
					Object dataProcessor = dataProcessorClass.getConstructor(ClassLoadHelper.class, boolean.class, boolean.class).newInstance(clh, true, true);
					Method processFileAndScheduleJobs = dataProcessorClass.getMethod("processFileAndScheduleJobs", String.class, Scheduler.class, boolean.class);
					for (String location : this.jobSchedulingDataLocations) {
						processFileAndScheduleJobs.invoke(dataProcessor, location, getScheduler(), this.overwriteExistingJobs);
					}
				}
			}

			// Register JobDetails.
			if (this.jobDetails != null) {
				for (JobDetail jobDetail : this.jobDetails) {
					addJobToScheduler(jobDetail);
				}
			}
			else {
				// Create empty list for easier checks when registering triggers.
				this.jobDetails = new LinkedList<JobDetail>();
			}

			// Register Calendars.
			if (this.calendars != null) {
				for (String calendarName : this.calendars.keySet()) {
					Calendar calendar = this.calendars.get(calendarName);
					getScheduler().addCalendar(calendarName, calendar, true, true);
				}
			}

			// Register Triggers.
			if (this.triggers != null) {
				for (Trigger trigger : this.triggers) {
					addTriggerToScheduler(trigger);
				}
			}
		}

		catch (Throwable ex) {
			if (transactionStatus != null) {
				try {
					this.transactionManager.rollback(transactionStatus);
				}
				catch (TransactionException tex) {
					logger.error("Job registration exception overridden by rollback exception", ex);
					throw tex;
				}
			}
			if (ex instanceof SchedulerException) {
				throw (SchedulerException) ex;
			}
			if (ex instanceof Exception) {
				throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage(), ex);
			}
			throw new SchedulerException("Registration of jobs and triggers failed: " + ex.getMessage());
		}

		if (transactionStatus != null) {
			this.transactionManager.commit(transactionStatus);
		}
	}

	/**
	 * Add the given job to the Scheduler, if it doesn't already exist.
	 * Overwrites the job in any case if "overwriteExistingJobs" is set.
	 * @param jobDetail the job to add
	 * @return {@code true} if the job was actually added,
	 * {@code false} if it already existed before
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addJobToScheduler(JobDetail jobDetail) throws SchedulerException {
		if (this.overwriteExistingJobs || !jobDetailExists(jobDetail)) {
			getScheduler().addJob(jobDetail, true);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Add the given trigger to the Scheduler, if it doesn't already exist.
	 * Overwrites the trigger in any case if "overwriteExistingJobs" is set.
	 * @param trigger the trigger to add
	 * @return {@code true} if the trigger was actually added,
	 * {@code false} if it already existed before
	 * @see #setOverwriteExistingJobs
	 */
	private boolean addTriggerToScheduler(Trigger trigger) throws SchedulerException {
		boolean triggerExists = triggerExists(trigger);
		if (!triggerExists || this.overwriteExistingJobs) {
			// Check if the Trigger is aware of an associated JobDetail.
			JobDetail jobDetail = findJobDetail(trigger);
			if (jobDetail != null) {
				// Automatically register the JobDetail too.
				if (!this.jobDetails.contains(jobDetail) && addJobToScheduler(jobDetail)) {
					this.jobDetails.add(jobDetail);
				}
			}
			if (!triggerExists) {
				try {
					getScheduler().scheduleJob(trigger);
				}
				catch (ObjectAlreadyExistsException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Unexpectedly found existing trigger, assumably due to cluster race condition: " +
								ex.getMessage() + " - can safely be ignored");
					}
					if (this.overwriteExistingJobs) {
						rescheduleJob(trigger);
					}
				}
			}
			else {
				rescheduleJob(trigger);
			}
			return true;
		}
		else {
			return false;
		}
	}

	private JobDetail findJobDetail(Trigger trigger) {
		if (trigger instanceof JobDetailAwareTrigger) {
			return ((JobDetailAwareTrigger) trigger).getJobDetail();
		}
		else {
			try {
				Map<?, ?> jobDataMap = (Map<?, ?>) ReflectionUtils.invokeMethod(Trigger.class.getMethod("getJobDataMap"), trigger);
				return (JobDetail) jobDataMap.remove(JobDetailAwareTrigger.JOB_DETAIL_KEY);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Inconsistent Quartz API: " + ex);
			}
		}
	}


	// Reflectively adapting to differences between Quartz 1.x and Quartz 2.0...
	private boolean jobDetailExists(JobDetail jobDetail) throws SchedulerException {
		if (jobKeyClass != null) {
			try {
				Method getJobDetail = Scheduler.class.getMethod("getJobDetail", jobKeyClass);
				Object key = ReflectionUtils.invokeMethod(JobDetail.class.getMethod("getKey"), jobDetail);
				return (ReflectionUtils.invokeMethod(getJobDetail, getScheduler(), key) != null);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Inconsistent Quartz 2.0 API: " + ex);
			}
		}
		else {
			return (getScheduler().getJobDetail(jobDetail.getName(), jobDetail.getGroup()) != null);
		}
	}

	// Reflectively adapting to differences between Quartz 1.x and Quartz 2.0...
	private boolean triggerExists(Trigger trigger) throws SchedulerException {
		if (triggerKeyClass != null) {
			try {
				Method getTrigger = Scheduler.class.getMethod("getTrigger", triggerKeyClass);
				Object key = ReflectionUtils.invokeMethod(Trigger.class.getMethod("getKey"), trigger);
				return (ReflectionUtils.invokeMethod(getTrigger, getScheduler(), key) != null);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Inconsistent Quartz 2.0 API: " + ex);
			}
		}
		else {
			return (getScheduler().getTrigger(trigger.getName(), trigger.getGroup()) != null);
		}
	}

	// Reflectively adapting to differences between Quartz 1.x and Quartz 2.0...
	private void rescheduleJob(Trigger trigger) throws SchedulerException {
		if (triggerKeyClass != null) {
			try {
				Method rescheduleJob = Scheduler.class.getMethod("rescheduleJob", triggerKeyClass, Trigger.class);
				Object key = ReflectionUtils.invokeMethod(Trigger.class.getMethod("getKey"), trigger);
				ReflectionUtils.invokeMethod(rescheduleJob, getScheduler(), key, trigger);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Inconsistent Quartz 2.0 API: " + ex);
			}
		}
		else {
			getScheduler().rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
		}
	}


	/**
	 * Register all specified listeners with the Scheduler.
	 */
	protected void registerListeners() throws SchedulerException {
		Object target;
		boolean quartz2;
		try {
			Method getListenerManager = Scheduler.class.getMethod("getListenerManager");
			target = ReflectionUtils.invokeMethod(getListenerManager, getScheduler());
			quartz2 = true;
		}
		catch (NoSuchMethodException ex) {
			target = getScheduler();
			quartz2 = false;
		}

		try {
			if (this.schedulerListeners != null) {
				Method addSchedulerListener = target.getClass().getMethod("addSchedulerListener", SchedulerListener.class);
				for (SchedulerListener listener : this.schedulerListeners) {
					ReflectionUtils.invokeMethod(addSchedulerListener, target, listener);
				}
			}
			if (this.globalJobListeners != null) {
				Method addJobListener = target.getClass().getMethod(
						(quartz2 ? "addJobListener" : "addGlobalJobListener"), JobListener.class);
				for (JobListener listener : this.globalJobListeners) {
					ReflectionUtils.invokeMethod(addJobListener, target, listener);
				}
			}
			if (this.jobListeners != null) {
				for (JobListener listener : this.jobListeners) {
					if (quartz2) {
						throw new IllegalStateException("Non-global JobListeners not supported on Quartz 2 - " +
								"manually register a Matcher against the Quartz ListenerManager instead");
					}
					getScheduler().addJobListener(listener);
				}
			}
			if (this.globalTriggerListeners != null) {
				Method addTriggerListener = target.getClass().getMethod(
						(quartz2 ? "addTriggerListener" : "addGlobalTriggerListener"), TriggerListener.class);
				for (TriggerListener listener : this.globalTriggerListeners) {
					ReflectionUtils.invokeMethod(addTriggerListener, target, listener);
				}
			}
			if (this.triggerListeners != null) {
				for (TriggerListener listener : this.triggerListeners) {
					if (quartz2) {
						throw new IllegalStateException("Non-global TriggerListeners not supported on Quartz 2 - " +
								"manually register a Matcher against the Quartz ListenerManager instead");
					}
					getScheduler().addTriggerListener(listener);
				}
			}
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("Expected Quartz API not present: " + ex);
		}
	}


	/**
	 * Template method that determines the Scheduler to operate on.
	 * To be implemented by subclasses.
	 */
	protected abstract Scheduler getScheduler();

}
