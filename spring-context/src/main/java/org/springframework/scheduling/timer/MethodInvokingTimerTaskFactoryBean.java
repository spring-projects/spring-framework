/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.scheduling.timer;

import java.util.TimerTask;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.scheduling.support.MethodInvokingRunnable;

/**
 * {@link FactoryBean} that exposes a {@link TimerTask} object which
 * delegates job execution to a specified (static or non-static) method.
 * Avoids the need to implement a one-line TimerTask that just invokes
 * an existing business method.
 *
 * <p>Derives from {@link MethodInvokingRunnable} to share common properties
 * and behavior, effectively providing a TimerTask adapter for it.
 *
 * @author Juergen Hoeller
 * @since 19.02.2004
 * @see DelegatingTimerTask
 * @see ScheduledTimerTask#setTimerTask
 * @see ScheduledTimerTask#setRunnable
 * @see org.springframework.scheduling.support.MethodInvokingRunnable
 * @see org.springframework.beans.factory.config.MethodInvokingFactoryBean
 * @deprecated as of Spring 3.0, in favor of the <code>scheduling.concurrent</code>
 * package which is based on Java 5's <code>java.util.concurrent.ExecutorService</code>
 */
@Deprecated
public class MethodInvokingTimerTaskFactoryBean extends MethodInvokingRunnable implements FactoryBean<TimerTask> {

	private TimerTask timerTask;


	@Override
	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		super.afterPropertiesSet();
		this.timerTask = new DelegatingTimerTask(this);
	}


	public TimerTask getObject() {
		return this.timerTask;
	}

	public Class<TimerTask> getObjectType() {
		return TimerTask.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
