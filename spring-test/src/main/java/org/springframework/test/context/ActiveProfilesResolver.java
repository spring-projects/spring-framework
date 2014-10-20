/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context;

/**
 * Strategy interface for programmatically resolving which <em>active bean
 * definition profiles</em> should be used when loading an
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for a test class.
 *
 * <p>A custom {@code ActiveProfilesResolver} can be registered via the
 * {@link ActiveProfiles#resolver resolver} attribute of {@code @ActiveProfiles}.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @since 4.0
 * @see ActiveProfiles
 */
public interface ActiveProfilesResolver {

	/**
	 * Resolve the <em>bean definition profiles</em> to use when loading an
	 * {@code ApplicationContext} for the given {@linkplain Class test class}.
	 * @param testClass the test class for which the profiles should be resolved;
	 * never {@code null}
	 * @return the list of bean definition profiles to use when loading the
	 * {@code ApplicationContext}; never {@code null}
	 * @see ActiveProfiles#resolver
	 * @see ActiveProfiles#inheritProfiles
	 */
	String[] resolve(Class<?> testClass);

}
