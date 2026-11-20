/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.reactive;

import java.net.http.HttpClient;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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

	private @Nullable Executor executor;

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
	public @Nullable Executor getExecutor() {
		return this.executor;
	}

	/**
	 * Configure the thread prefix to initialize the executor with. This
	 * is used only when a {@link Executor} instance isn't
	 * {@link #setExecutor(Executor) provided}.
	 * <p>By default set to "jdk-http".
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
	}

	@Override
	public void destroy() throws Exception {
		if (this.executor instanceof ExecutorService executorService) {
			executorService.shutdown();
		}
	}

}
