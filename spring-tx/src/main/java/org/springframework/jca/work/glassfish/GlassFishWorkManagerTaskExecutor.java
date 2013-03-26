/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jca.work.glassfish;

import java.lang.reflect.Method;
import javax.resource.spi.work.WorkManager;

import org.springframework.jca.work.WorkManagerTaskExecutor;
import org.springframework.util.ReflectionUtils;

/**
 * Spring TaskExecutor adapter for the GlassFish JCA WorkManager.
 * Can be defined in web applications to make a TaskExecutor reference
 * available, talking to the GlassFish WorkManager (thread pool) underneath.
 *
 * <p>This is the GlassFish equivalent of the CommonJ
 * {@link org.springframework.scheduling.commonj.WorkManagerTaskExecutor}
 * adapter for WebLogic and WebSphere.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class GlassFishWorkManagerTaskExecutor extends WorkManagerTaskExecutor {

	private static final String WORK_MANAGER_FACTORY_CLASS = "com.sun.enterprise.connectors.work.WorkManagerFactory";

	private final Method getWorkManagerMethod;


	public GlassFishWorkManagerTaskExecutor() {
		try {
			Class wmf = getClass().getClassLoader().loadClass(WORK_MANAGER_FACTORY_CLASS);
			this.getWorkManagerMethod = wmf.getMethod("getWorkManager", new Class[] {String.class});
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize GlassFishWorkManagerTaskExecutor because GlassFish API is not available: " + ex);
		}
	}

	/**
	 * Identify a specific GlassFish thread pool to talk to.
	 * <p>The thread pool name matches the resource adapter name
	 * in default RAR deployment scenarios.
	 */
	public void setThreadPoolName(String threadPoolName) {
		WorkManager wm = (WorkManager) ReflectionUtils.invokeMethod(this.getWorkManagerMethod, null, threadPoolName);
		if (wm == null) {
			throw new IllegalArgumentException("Specified thread pool name '" + threadPoolName +
					"' does not correspond to an actual pool definition in GlassFish. Check your configuration!");
		}
		setWorkManager(wm);
	}

	/**
	 * Obtains GlassFish's default thread pool.
	 */
	@Override
	protected WorkManager getDefaultWorkManager() {
		return (WorkManager) ReflectionUtils.invokeMethod(this.getWorkManagerMethod, null, new Object[] {null});
	}

}
