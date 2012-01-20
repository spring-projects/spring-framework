/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.jca.work.jboss;

import javax.resource.spi.work.WorkManager;

import org.springframework.jca.work.WorkManagerTaskExecutor;

/**
 * Spring TaskExecutor adapter for the JBoss JCA WorkManager.
 * Can be defined in web applications to make a TaskExecutor reference
 * available, talking to the JBoss WorkManager (thread pool) underneath.
 *
 * <p>This is the JBoss equivalent of the CommonJ
 * {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor}
 * adapter for WebLogic and WebSphere.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.jboss.resource.work.JBossWorkManagerMBean
 */
public class JBossWorkManagerTaskExecutor extends WorkManagerTaskExecutor {

	/**
	 * Identify a specific JBossWorkManagerMBean to talk to,
	 * through its JMX object name.
	 * <p>The default MBean name is "jboss.jca:service=WorkManager".
	 * @see JBossWorkManagerUtils#getWorkManager(String)
	 */
	public void setWorkManagerMBeanName(String mbeanName) {
		setWorkManager(JBossWorkManagerUtils.getWorkManager(mbeanName));
	}

	/**
	 * Obtains the default JBoss JCA WorkManager through a JMX lookup
	 * for the JBossWorkManagerMBean.
	 * @see JBossWorkManagerUtils#getWorkManager()
	 */
	@Override
	protected WorkManager getDefaultWorkManager() {
		return JBossWorkManagerUtils.getWorkManager();
	}

}
