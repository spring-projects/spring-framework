/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jca.support;

import java.util.Timer;

import javax.resource.spi.BootstrapContext;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.work.WorkManager;

/**
 * Simple implementation of the JCA 1.5 {@link javax.resource.spi.BootstrapContext}
 * interface, used for bootstrapping a JCA ResourceAdapter in a local environment.
 *
 * <p>Delegates to the given WorkManager and XATerminator, if any. Creates simple
 * local instances of {@code java.util.Timer}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
 * @see ResourceAdapterFactoryBean
 */
public class SimpleBootstrapContext implements BootstrapContext {

	private WorkManager workManager;

	private XATerminator xaTerminator;


	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager,
	 * with no XATerminator available.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 */
	public SimpleBootstrapContext(WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager and XATerminator.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 * @param xaTerminator the JCA XATerminator to use (may be {@code null})
	 */
	public SimpleBootstrapContext(WorkManager workManager, XATerminator xaTerminator) {
		this.workManager = workManager;
		this.xaTerminator = xaTerminator;
	}


	public WorkManager getWorkManager() {
		if (this.workManager == null) {
			throw new IllegalStateException("No WorkManager available");
		}
		return this.workManager;
	}

	public XATerminator getXATerminator() {
		return this.xaTerminator;
	}

	public Timer createTimer() throws UnavailableException {
		return new Timer();
	}

}
