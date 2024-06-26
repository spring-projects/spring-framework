/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.transaction.reactive;

import org.springframework.core.ReactiveAdapterRegistry;

/**
 * Service provider interface (SPI) that allows to customize usage of reactive type adapters in Spring Transactions.
 * <p/>
 * By default, Spring Transactions uses the global registry provided by
 * {@link ReactiveAdapterRegistry#getSharedInstance()}, but this behavior can be changed by implementing this provider
 * and <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html#deploying-service-providers-on-the-class-path-heading">deploying it on a class path</a>.
 * <p/>
 * The implementations of the SPI should be threadsafe.
 *
 * @author Bohdan Pryshedko
 */
public interface ReactiveAdapterRegistryProvider {
	/**
	 * Returns instance of the registry that will be used by Spring Transactions.
	 * <p/>
	 * The implementation shouldn't make any assumption about when it is called, how much time it can be called, and if
	 * the result is cached or not.
	 * @return registry to be used by Spring Transactions.
	 */
	ReactiveAdapterRegistry get();
}
