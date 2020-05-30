/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.http.client.reactive;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link ReactorResourceFactory}.
 * @author Rossen Stoyanchev
 */
public class ReactorResourceFactoryTests {

	private final ReactorResourceFactory resourceFactory = new ReactorResourceFactory();

	private final ConnectionProvider connectionProvider = mock(ConnectionProvider.class);

	private final LoopResources loopResources = mock(LoopResources.class);


	@Test
	void globalResources() throws Exception {

		this.resourceFactory.setUseGlobalResources(true);
		this.resourceFactory.afterPropertiesSet();

		HttpResources globalResources = HttpResources.get();
		assertThat(this.resourceFactory.getConnectionProvider()).isSameAs(globalResources);
		assertThat(this.resourceFactory.getLoopResources()).isSameAs(globalResources);
		assertThat(globalResources.isDisposed()).isFalse();

		this.resourceFactory.destroy();

		assertThat(globalResources.isDisposed()).isTrue();
	}

	@Test
	void globalResourcesWithConsumer() throws Exception {

		AtomicBoolean invoked = new AtomicBoolean(false);

		this.resourceFactory.addGlobalResourcesConsumer(httpResources -> invoked.set(true));
		this.resourceFactory.afterPropertiesSet();

		assertThat(invoked.get()).isTrue();
		this.resourceFactory.destroy();
	}

	@Test
	void localResources() throws Exception {

		this.resourceFactory.setUseGlobalResources(false);
		this.resourceFactory.afterPropertiesSet();

		ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
		LoopResources loopResources = this.resourceFactory.getLoopResources();

		assertThat(connectionProvider).isNotSameAs(HttpResources.get());
		assertThat(loopResources).isNotSameAs(HttpResources.get());

		// The below does not work since ConnectionPoolProvider simply checks if pool is empty.
		// assertFalse(connectionProvider.isDisposed());
		assertThat(loopResources.isDisposed()).isFalse();

		this.resourceFactory.destroy();

		assertThat(connectionProvider.isDisposed()).isTrue();
		assertThat(loopResources.isDisposed()).isTrue();
	}

	@Test
	void localResourcesViaSupplier() throws Exception {

		this.resourceFactory.setUseGlobalResources(false);
		this.resourceFactory.setConnectionProviderSupplier(() -> this.connectionProvider);
		this.resourceFactory.setLoopResourcesSupplier(() -> this.loopResources);
		this.resourceFactory.afterPropertiesSet();

		ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
		LoopResources loopResources = this.resourceFactory.getLoopResources();

		assertThat(connectionProvider).isSameAs(this.connectionProvider);
		assertThat(loopResources).isSameAs(this.loopResources);

		verifyNoMoreInteractions(this.connectionProvider, this.loopResources);

		this.resourceFactory.destroy();

		// Managed (destroy disposes)..
		verify(this.connectionProvider).disposeLater();
		verify(this.loopResources).disposeLater(eq(Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_QUIET_PERIOD)), eq(Duration.ofSeconds(LoopResources.DEFAULT_SHUTDOWN_TIMEOUT)));
		verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
	}

	@Test
	void customShutdownDurations() throws Exception {
		Duration quietPeriod = Duration.ofMillis(500);
		Duration shutdownTimeout = Duration.ofSeconds(1);
		this.resourceFactory.setUseGlobalResources(false);
		this.resourceFactory.setConnectionProviderSupplier(() -> this.connectionProvider);
		this.resourceFactory.setLoopResourcesSupplier(() -> this.loopResources);
		this.resourceFactory.setShutdownQuietPeriod(quietPeriod);
		this.resourceFactory.setShutdownTimeout(shutdownTimeout);
		this.resourceFactory.afterPropertiesSet();
		this.resourceFactory.destroy();

		verify(this.connectionProvider).disposeLater();
		verify(this.loopResources).disposeLater(eq(quietPeriod), eq(shutdownTimeout));
		verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
	}

	@Test
	void externalResources() throws Exception {

		this.resourceFactory.setUseGlobalResources(false);
		this.resourceFactory.setConnectionProvider(this.connectionProvider);
		this.resourceFactory.setLoopResources(this.loopResources);
		this.resourceFactory.afterPropertiesSet();

		ConnectionProvider connectionProvider = this.resourceFactory.getConnectionProvider();
		LoopResources loopResources = this.resourceFactory.getLoopResources();

		assertThat(connectionProvider).isSameAs(this.connectionProvider);
		assertThat(loopResources).isSameAs(this.loopResources);

		verifyNoMoreInteractions(this.connectionProvider, this.loopResources);

		this.resourceFactory.destroy();

		// Not managed (destroy has no impact)..
		verifyNoMoreInteractions(this.connectionProvider, this.loopResources);
	}

}
