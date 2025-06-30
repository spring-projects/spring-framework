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

package org.springframework.jca.support;

import java.util.Timer;

import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.XATerminator;
import jakarta.resource.spi.work.WorkContext;
import jakarta.resource.spi.work.WorkManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Simple implementation of the JCA 1.7 {@link jakarta.resource.spi.BootstrapContext}
 * interface, used for bootstrapping a JCA ResourceAdapter in a local environment.
 *
 * <p>Delegates to the given WorkManager and XATerminator, if any. Creates simple
 * local instances of {@code java.util.Timer}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see jakarta.resource.spi.ResourceAdapter#start(jakarta.resource.spi.BootstrapContext)
 * @see ResourceAdapterFactoryBean
 */
public class SimpleBootstrapContext implements BootstrapContext {

	private final @Nullable WorkManager workManager;

	private @Nullable XATerminator xaTerminator;

	private @Nullable TransactionSynchronizationRegistry transactionSynchronizationRegistry;


	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager,
	 * with no XATerminator available.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 */
	public SimpleBootstrapContext(@Nullable WorkManager workManager) {
		this.workManager = workManager;
	}

	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager and XATerminator.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 * @param xaTerminator the JCA XATerminator to use (may be {@code null})
	 */
	public SimpleBootstrapContext(@Nullable WorkManager workManager, @Nullable XATerminator xaTerminator) {
		this.workManager = workManager;
		this.xaTerminator = xaTerminator;
	}

	/**
	 * Create a new SimpleBootstrapContext for the given WorkManager, XATerminator
	 * and TransactionSynchronizationRegistry.
	 * @param workManager the JCA WorkManager to use (may be {@code null})
	 * @param xaTerminator the JCA XATerminator to use (may be {@code null})
	 * @param transactionSynchronizationRegistry the TransactionSynchronizationRegistry
	 * to use (may be {@code null})
	 * @since 5.0
	 */
	public SimpleBootstrapContext(@Nullable WorkManager workManager, @Nullable XATerminator xaTerminator,
			@Nullable TransactionSynchronizationRegistry transactionSynchronizationRegistry) {

		this.workManager = workManager;
		this.xaTerminator = xaTerminator;
		this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
	}


	@Override
	public WorkManager getWorkManager() {
		Assert.state(this.workManager != null, "No WorkManager available");
		return this.workManager;
	}

	@Override
	public @Nullable XATerminator getXATerminator() {
		return this.xaTerminator;
	}

	@Override
	public Timer createTimer() throws UnavailableException {
		return new Timer();
	}

	@Override
	public boolean isContextSupported(Class<? extends WorkContext> workContextClass) {
		return false;
	}

	@Override
	public @Nullable TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
		return this.transactionSynchronizationRegistry;
	}

}
