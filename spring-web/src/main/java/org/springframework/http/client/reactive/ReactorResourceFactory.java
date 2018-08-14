/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.http.client.reactive;

import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory to manage Reactor Netty resources, i.e. {@link LoopResources} for
 * event loop threads, and {@link ConnectionProvider} for the connection pool,
 * within the lifecycle of a Spring {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean} and {@link DisposableBean}
 * and is expected typically to be declared as a Spring-managed bean.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class ReactorResourceFactory implements InitializingBean, DisposableBean {

	private boolean globalResources = true;

	@Nullable
	private ConnectionProvider connectionProvider;

	@Nullable
	private LoopResources loopResources;

	private String threadPrefix = "reactor-http";


	/**
	 * Whether to expose and manage the global Reactor Netty resources from the
	 * {@link HttpResources} holder.
	 * <p>Default is "true" in which case this factory helps to configure and
	 * shut down the global Reactor Netty resources within the lifecycle of a
	 * Spring {@code ApplicationContext}.
	 * <p>If set to "false" then the factory creates and manages its own
	 * {@link LoopResources} and {@link ConnectionProvider}, independent of the
	 * global ones in the {@link HttpResources} holder.
	 * @param globalResources whether to expose and manage the global resources
	 */
	public void setGlobalResources(boolean globalResources) {
		this.globalResources = globalResources;
	}

	/**
	 * Configure the {@link ConnectionProvider} to use.
	 * <p>By default, initialized with {@link ConnectionProvider#elastic(String)}.
	 * @param connectionProvider the connection provider to use
	 */
	public void setConnectionProvider(@Nullable ConnectionProvider connectionProvider) {
		this.connectionProvider = connectionProvider;
	}

	/**
	 * Configure the {@link LoopResources} to use.
	 * <p>By default, initialized with {@link LoopResources#create(String)}.
	 * @param loopResources the loop resources to use
	 */
	public void setLoopResources(@Nullable LoopResources loopResources) {
		this.loopResources = loopResources;
	}

	/**
	 * Configure the thread prefix to initialize {@link LoopResources} with. This
	 * is used only when a {@link LoopResources} instance isn't
	 * {@link #setLoopResources(LoopResources) provided}.
	 * <p>By default set to "reactor-http".
	 * @param threadPrefix the thread prefix to use
	 */
	public void setThreadPrefix(String threadPrefix) {
		Assert.notNull(threadPrefix, "Thread prefix is required");
		this.threadPrefix = threadPrefix;
	}


	/**
	 * Whether this factory exposes the global
	 * {@link reactor.netty.http.HttpResources HttpResources} holder.
	 */
	public boolean isGlobalResources() {
		return this.globalResources;
	}

	/**
	 * Return the configured {@link ConnectionProvider}.
	 */
	@Nullable
	public ConnectionProvider getConnectionProvider() {
		return this.connectionProvider;
	}

	/**
	 * Return the configured {@link LoopResources}.
	 */
	@Nullable
	public LoopResources getLoopResources() {
		return this.loopResources;
	}

	/**
	 * Return the configured prefix for event loop threads.
	 */
	public String getThreadPrefix() {
		return this.threadPrefix;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.loopResources == null) {
			this.loopResources = LoopResources.create(this.threadPrefix);
		}
		if (this.connectionProvider == null) {
			this.connectionProvider = ConnectionProvider.elastic("http");
		}
		if (this.globalResources) {
			HttpResources.set(this.loopResources);
			HttpResources.set(this.connectionProvider);
		}
	}

	@Override
	public void destroy() {

		try {
			ConnectionProvider provider = this.connectionProvider;
			if (provider != null) {
				provider.dispose();
			}
		}
		catch (Throwable ex) {
			// ignore
		}

		try {
			LoopResources resources = this.loopResources;
			if (resources != null) {
				resources.dispose();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
	}

}
