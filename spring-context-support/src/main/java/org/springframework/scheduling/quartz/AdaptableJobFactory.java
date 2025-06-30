/*
 * Copyright 2002-present the original author or authors.
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

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import org.springframework.util.ReflectionUtils;

/**
 * {@link JobFactory} implementation that supports {@link java.lang.Runnable}
 * objects as well as standard Quartz {@link org.quartz.Job} instances.
 *
 * <p>Compatible with Quartz 2.1.4 and higher, as of Spring 4.1.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DelegatingJob
 * @see #adaptJob(Object)
 */
public class AdaptableJobFactory implements JobFactory {

	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		try {
			Object jobObject = createJobInstance(bundle);
			return adaptJob(jobObject);
		}
		catch (Throwable ex) {
			throw new SchedulerException("Job instantiation failed", ex);
		}
	}

	/**
	 * Create an instance of the specified job class.
	 * <p>Can be overridden to post-process the job instance.
	 * @param bundle the TriggerFiredBundle from which the JobDetail
	 * and other info relating to the trigger firing can be obtained
	 * @return the job instance
	 * @throws Exception if job instantiation failed
	 */
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Class<?> jobClass = bundle.getJobDetail().getJobClass();
		return ReflectionUtils.accessibleConstructor(jobClass).newInstance();
	}

	/**
	 * Adapt the given job object to the Quartz Job interface.
	 * <p>The default implementation supports straight Quartz Jobs
	 * as well as Runnables, which get wrapped in a DelegatingJob.
	 * @param jobObject the original instance of the specified job class
	 * @return the adapted Quartz Job instance
	 * @throws Exception if the given job could not be adapted
	 * @see DelegatingJob
	 */
	protected Job adaptJob(Object jobObject) throws Exception {
		if (jobObject instanceof Job job) {
			return job;
		}
		else if (jobObject instanceof Runnable runnable) {
			return new DelegatingJob(runnable);
		}
		else {
			throw new IllegalArgumentException(
					"Unable to execute job class [" + jobObject.getClass().getName() +
					"]: only [org.quartz.Job] and [java.lang.Runnable] supported.");
		}
	}

}
