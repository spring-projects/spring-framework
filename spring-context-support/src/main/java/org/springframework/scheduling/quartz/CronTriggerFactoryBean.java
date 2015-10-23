/*
 * Copyright 2002-2014 the original author or authors.
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

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.impl.triggers.CronTriggerImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.util.Assert;

/**
 * A Spring {@link FactoryBean} for creating a Quartz {@link org.quartz.CronTrigger}
 * instance, supporting bean-style usage for trigger configuration.
 *
 * <p>{@code CronTrigger(Impl)} itself is already a JavaBean but lacks sensible defaults.
 * This class uses the Spring bean name as job name, the Quartz default group ("DEFAULT")
 * as job group, the current time as start time, and indefinite repetition, if not specified.
 *
 * <p>This class will also register the trigger with the job name and group of
 * a given {@link org.quartz.JobDetail}. This allows {@link SchedulerFactoryBean}
 * to automatically register a trigger for the corresponding JobDetail,
 * instead of registering the JobDetail separately.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see #setName
 * @see #setGroup
 * @see #setStartDelay
 * @see #setJobDetail
 * @see SchedulerFactoryBean#setTriggers
 * @see SchedulerFactoryBean#setJobDetails
 */
public class CronTriggerFactoryBean implements FactoryBean<CronTrigger>, BeanNameAware, InitializingBean {

	/** Constants for the CronTrigger class */
	private static final Constants constants = new Constants(CronTrigger.class);


	private String name;

	private String group;

	private JobDetail jobDetail;

	private JobDataMap jobDataMap = new JobDataMap();

	private Date startTime;

	private long startDelay = 0;

	private String cronExpression;

	private TimeZone timeZone;

	private String calendarName;

	private int priority;

	private int misfireInstruction;

	private String description;

	private String beanName;

	private CronTrigger cronTrigger;


	/**
	 * Specify the trigger's name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Specify the trigger's group.
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * Set the JobDetail that this trigger should be associated with.
	 */
	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	/**
	 * Set the trigger's JobDataMap.
	 * @see #setJobDataAsMap
	 */
	public void setJobDataMap(JobDataMap jobDataMap) {
		this.jobDataMap = jobDataMap;
	}

	/**
	 * Return the trigger's JobDataMap.
	 */
	public JobDataMap getJobDataMap() {
		return this.jobDataMap;
	}

	/**
	 * Register objects in the JobDataMap via a given Map.
	 * <p>These objects will be available to this Trigger only,
	 * in contrast to objects in the JobDetail's data map.
	 * @param jobDataAsMap Map with String keys and any objects as values
	 * (for example Spring-managed beans)
	 */
	public void setJobDataAsMap(Map<String, ?> jobDataAsMap) {
		this.jobDataMap.putAll(jobDataAsMap);
	}

	/**
	 * Set a specific start time for the trigger.
	 * <p>Note that a dynamically computed {@link #setStartDelay} specification
	 * overrides a static timestamp set here.
	 */
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	/**
	 * Set the start delay in milliseconds.
	 * <p>The start delay is added to the current system time (when the bean starts)
	 * to control the start time of the trigger.
	 */
	public void setStartDelay(long startDelay) {
		Assert.isTrue(startDelay >= 0, "Start delay cannot be negative");
		this.startDelay = startDelay;
	}

	/**
	 * Specify the cron expression for this trigger.
	 */
	public void setCronExpression(String cronExpression) {
		this.cronExpression = cronExpression;
	}

	/**
	 * Specify the time zone for this trigger's cron expression.
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Associate a specific calendar with this cron trigger.
	 */
	public void setCalendarName(String calendarName) {
		this.calendarName = calendarName;
	}

	/**
	 * Specify the priority of this trigger.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Specify a misfire instruction for this trigger.
	 */
	public void setMisfireInstruction(int misfireInstruction) {
		this.misfireInstruction = misfireInstruction;
	}

	/**
	 * Set the misfire instruction via the name of the corresponding
	 * constant in the {@link org.quartz.CronTrigger} class.
	 * Default is {@code MISFIRE_INSTRUCTION_SMART_POLICY}.
	 * @see org.quartz.CronTrigger#MISFIRE_INSTRUCTION_FIRE_ONCE_NOW
	 * @see org.quartz.CronTrigger#MISFIRE_INSTRUCTION_DO_NOTHING
	 * @see org.quartz.Trigger#MISFIRE_INSTRUCTION_SMART_POLICY
	 */
	public void setMisfireInstructionName(String constantName) {
		this.misfireInstruction = constants.asNumber(constantName).intValue();
	}

	/**
	 * Associate a textual description with this trigger.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}


	@Override
	public void afterPropertiesSet() throws ParseException {
		if (this.name == null) {
			this.name = this.beanName;
		}
		if (this.group == null) {
			this.group = Scheduler.DEFAULT_GROUP;
		}
		if (this.jobDetail != null) {
			this.jobDataMap.put("jobDetail", this.jobDetail);
		}
		if (this.startDelay > 0 || this.startTime == null) {
			this.startTime = new Date(System.currentTimeMillis() + this.startDelay);
		}
		if (this.timeZone == null) {
			this.timeZone = TimeZone.getDefault();
		}

		CronTriggerImpl cti = new CronTriggerImpl();
		cti.setName(this.name);
		cti.setGroup(this.group);
		if (this.jobDetail != null) {
			cti.setJobKey(this.jobDetail.getKey());
			cti.setJobDataMap(this.jobDataMap);
		}
		cti.setStartTime(this.startTime);
		cti.setCronExpression(this.cronExpression);
		cti.setTimeZone(this.timeZone);
		cti.setCalendarName(this.calendarName);
		cti.setPriority(this.priority);
		cti.setMisfireInstruction(this.misfireInstruction);
		cti.setDescription(this.description);
		this.cronTrigger = cti;
	}


	@Override
	public CronTrigger getObject() {
		return this.cronTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return CronTrigger.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
