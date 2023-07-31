/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Date;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A Spring {@link FactoryBean} for creating a Quartz {@link org.quartz.SimpleTrigger}
 * instance, supporting bean-style usage for trigger configuration.
 *
 * <p>{@code SimpleTrigger(Impl)} itself is already a JavaBean but lacks sensible defaults.
 * This class uses the Spring bean name as job name, the Quartz default group ("DEFAULT")
 * as job group, the current time as start time, and indefinite repetition, if not specified.
 *
 * <p>This class will also register the trigger with the job name and group of
 * a given {@link org.quartz.JobDetail}. This allows {@link SchedulerFactoryBean}
 * to automatically register a trigger for the corresponding JobDetail,
 * instead of registering the JobDetail separately.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see #setName
 * @see #setGroup
 * @see #setStartDelay
 * @see #setJobDetail
 * @see SchedulerFactoryBean#setTriggers
 * @see SchedulerFactoryBean#setJobDetails
 */
public class SimpleTriggerFactoryBean implements FactoryBean<SimpleTrigger>, BeanNameAware, InitializingBean {

	/**
	 * Map of constant names to constant values for the misfire instruction constants
	 * defined in {@link org.quartz.Trigger} and {@link org.quartz.SimpleTrigger}.
	 */
	private static final Map<String, Integer> constants = Map.of(
			"MISFIRE_INSTRUCTION_SMART_POLICY",
				SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY,
			"MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY",
				SimpleTrigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
			"MISFIRE_INSTRUCTION_FIRE_NOW",
				SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW,
			"MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT",
				SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT,
			"MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT",
				SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
			"MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT",
				SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT,
			"MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT",
				SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
		);

	@Nullable
	private String name;

	@Nullable
	private String group;

	@Nullable
	private JobDetail jobDetail;

	private JobDataMap jobDataMap = new JobDataMap();

	@Nullable
	private Date startTime;

	private long startDelay;

	private long repeatInterval;

	private int repeatCount = -1;

	private int priority;

	private int misfireInstruction = SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY;

	@Nullable
	private String description;

	@Nullable
	private String beanName;

	@Nullable
	private SimpleTrigger simpleTrigger;


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
	 * @param jobDataAsMap a Map with String keys and any objects as values
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
	 * @see #setStartTime
	 */
	public void setStartDelay(long startDelay) {
		Assert.isTrue(startDelay >= 0, "Start delay cannot be negative");
		this.startDelay = startDelay;
	}

	/**
	 * Specify the interval between execution times of this trigger.
	 */
	public void setRepeatInterval(long repeatInterval) {
		this.repeatInterval = repeatInterval;
	}

	/**
	 * Specify the number of times this trigger is supposed to fire.
	 * <p>Default is to repeat indefinitely.
	 */
	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	/**
	 * Specify the priority of this trigger.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Specify the misfire instruction for this trigger.
	 */
	public void setMisfireInstruction(int misfireInstruction) {
		Assert.isTrue(constants.containsValue(misfireInstruction),
				"Only values of misfire instruction constants allowed");
		this.misfireInstruction = misfireInstruction;
	}

	/**
	 * Set the misfire instruction for this trigger via the name of the corresponding
	 * constant in the {@link org.quartz.Trigger} and {@link org.quartz.SimpleTrigger}
	 * classes.
	 * <p>Default is {@code MISFIRE_INSTRUCTION_SMART_POLICY}.
	 * @see org.quartz.Trigger#MISFIRE_INSTRUCTION_SMART_POLICY
	 * @see org.quartz.Trigger#MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_FIRE_NOW
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT
	 * @see org.quartz.SimpleTrigger#MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT
	 */
	public void setMisfireInstructionName(String constantName) {
		Assert.hasText(constantName, "'constantName' must not be null or blank");
		Integer misfireInstruction = constants.get(constantName);
		Assert.notNull(misfireInstruction, "Only misfire instruction constants allowed");
		this.misfireInstruction = misfireInstruction;
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
	public void afterPropertiesSet() {
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

		SimpleTriggerImpl sti = new SimpleTriggerImpl();
		sti.setName(this.name != null ? this.name : toString());
		sti.setGroup(this.group);
		if (this.jobDetail != null) {
			sti.setJobKey(this.jobDetail.getKey());
		}
		sti.setJobDataMap(this.jobDataMap);
		sti.setStartTime(this.startTime);
		sti.setRepeatInterval(this.repeatInterval);
		sti.setRepeatCount(this.repeatCount);
		sti.setPriority(this.priority);
		sti.setMisfireInstruction(this.misfireInstruction);
		sti.setDescription(this.description);
		this.simpleTrigger = sti;
	}


	@Override
	@Nullable
	public SimpleTrigger getObject() {
		return this.simpleTrigger;
	}

	@Override
	public Class<?> getObjectType() {
		return SimpleTrigger.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
