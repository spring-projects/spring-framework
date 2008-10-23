/*
 * Copyright 2002-2005 the original author or authors.
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
 * FactoryBean that exposes a TimerTask object that delegates
 * job execution to a specified (static or non-static) method.
 * Avoids the need to implement a one-line TimerTask that just
 * invokes an existing business method.
 *
 * <p>Derives from MethodInvokingRunnable to share common properties
 * and behavior, effectively providing a TimerTask adapter for it.
 *
 * <p>Often used to populate a ScheduledTimerTask object with a specific
 * reflective method invocation. Note that you can alternatively populate
 * a ScheduledTimerTask object with a plain MethodInvokingRunnable instance
 * as well (as of Spring 1.2.4), without the need for this special FactoryBean.
 *
 * @author Juergen Hoeller
 * @since 19.02.2004
 * @see DelegatingTimerTask
 * @see ScheduledTimerTask#setTimerTask
 * @see ScheduledTimerTask#setRunnable
 * @see org.springframework.scheduling.support.MethodInvokingRunnable
 * @see org.springframework.beans.factory.config.MethodInvokingFactoryBean
 */
public class MethodInvokingTimerTaskFactoryBean extends MethodInvokingRunnable implements FactoryBean {

	private TimerTask timerTask;


	public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
		super.afterPropertiesSet();
		this.timerTask = new DelegatingTimerTask(this);
	}


	public Object getObject() {
		return this.timerTask;
	}

	public Class getObjectType() {
		return TimerTask.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
