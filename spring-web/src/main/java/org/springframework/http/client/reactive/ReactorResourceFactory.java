/*
 * Copyright 2002-2023 the original author or authors.
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

import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;

/**
 * Factory to manage Reactor Netty resources, i.e. {@link LoopResources} for
 * event loop threads, and {@link ConnectionProvider} for the connection pool,
 * within the lifecycle of a Spring {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean}, {@link DisposableBean}
 * and {@link Lifecycle} and is expected typically to be declared as a
 * Spring-managed bean.
 *
 * <p>Notice that after a {@link Lifecycle} stop/restart, new instances of
 * the configured {@link LoopResources} and {@link ConnectionProvider} are
 * created, so any references to those should be updated.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 5.1
 * @deprecated since 6.1 due to a package change; use {@link org.springframework.http.client.ReactorResourceFactory} instead.
 */
@Deprecated(since = "6.1")
public class ReactorResourceFactory extends org.springframework.http.client.ReactorResourceFactory {
}
