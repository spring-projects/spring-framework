/*
 * Copyright 2002-2021 the original author or authors.
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

import java.net.http.HttpClient;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * Factory to manage JDK HttpClient resources such as a shared {@link Executor}
 * within the lifecycle of a Spring {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean} and {@link DisposableBean}
 * and is expected typically to be declared as a Spring-managed bean.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see JdkClientHttpConnector#JdkClientHttpConnector(HttpClient.Builder, JdkHttpClientResourceFactory)
 */
public class JdkHttpClientResourceFactory implements InitializingBean, DisposableBean {

	@Nullable
	private Executor executor;

	private String threadPrefix = "jdk-http";


	/**
	 * Configure the {@link Executor} to use for {@link HttpClient} exchanges.
	 * The given executor is started and stopped via {@link InitializingBean}
	 * and {@link DisposableBean}.
	 * <p>By default, this is set to {@link Executors#newCachedThreadPool(ThreadFactory)},
	 * which mirrors {@link HttpClient.Builder#executor(Executor)}.
	 * @param executor the executor to use
	 */
	public void setExecutor(@Nullable Executor executor) {
		this.executor = executor;
	}

	/**
	 * Return the configured {@link Executor}.
	 */
	@Nullable
	public Executor getExecutor() {
		return this.executor;
	}

	/**
	 * Configure the thread prefix to initialize {@link QueuedThreadPool} executor with. This
	 * is used only when a {@link Executor} instance isn't
	 * {@link #setExecutor(Executor) provided}.
	 * <p>By default set to "jetty-http".
	 * @param threadPrefix the thread prefix to use
	 */
	public void setThreadPrefix(String threadPrefix) {
		Assert.notNull(threadPrefix, "Thread prefix is required");
		this.threadPrefix = threadPrefix;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.executor == null) {
			String name = this.threadPrefix + "@" + Integer.toHexString(hashCode());
			this.executor = Executors.newCachedThreadPool(new CustomizableThreadFactory(name));
		}
		if (this.executor instanceof LifeCycle) {
			((LifeCycle)this.executor).start();
		}
	}

	@Override
	public void destroy() throws Exception {
		try {
			if (this.executor instanceof LifeCycle) {
				((LifeCycle)this.executor).stop();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
	}

}
