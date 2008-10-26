/*
 * Copyright 2002-2006 the original author or authors.
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

import org.quartz.SchedulerContext;

/**
 * Callback interface to be implemented by Spring-managed
 * Quartz artifacts that need access to the SchedulerContext
 * (without having natural access to it).
 *
 * <p>Currently only supported for custom JobFactory implementations
 * that are passed in via Spring's SchedulerFactoryBean.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.quartz.spi.JobFactory
 * @see SchedulerFactoryBean#setJobFactory
 */
public interface SchedulerContextAware {

	/**
	 * Set the SchedulerContext of the current Quartz Scheduler.
	 * @see org.quartz.Scheduler#getContext()
	 */
	void setSchedulerContext(SchedulerContext schedulerContext);

}
