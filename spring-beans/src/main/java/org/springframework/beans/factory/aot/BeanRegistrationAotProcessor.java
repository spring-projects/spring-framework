/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.lang.Nullable;

/**
 * AOT processor that makes bean registration contributions by processing
 * {@link RegisteredBean} instances.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see BeanRegistrationAotContribution
 */
@FunctionalInterface
public interface BeanRegistrationAotProcessor {

	/**
	 * Process the given {@link RegisteredBean} instance ahead-of-time and
	 * return a contribution or {@code null}.
	 * <p>
	 * Processors are free to use any techniques they like to analyze the given
	 * instance. Most typically use reflection to find fields or methods to use
	 * in the contribution. Contributions typically generate source code or
	 * resource files that can be used when the AOT optimized application runs.
	 * <p>
	 * If the given instance isn't relevant to the processor, it should return a
	 * {@code null} contribution.
	 * @param registeredBean the registered bean to process
	 * @return a {@link BeanRegistrationAotContribution} or {@code null}
	 */
	@Nullable
	BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean);

}
