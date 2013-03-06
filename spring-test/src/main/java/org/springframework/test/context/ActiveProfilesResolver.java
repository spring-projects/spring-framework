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
 * {@code ActiveProfiles} is an interface that is used together with
 * {@link ActiveProfiles#resolver} to decide which <em>active bean definition
 * profiles</em> should be used when loading an
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * for test classes.
 *
 * <p>{@link ActiveProfiles#resolver} attribute may <strong>not</strong> be used in
 * conjunction with {@link ActiveProfiles#value} neither {@link ActiveProfiles#profiles}
 * for the same {@link ActiveProfiles} annotation, but it may be used <em>instead</em>
 * of them.
 *
 * <p>If it is required to mix compile-time and runtime profile declarations, then it is
 * possible:
 * <ul>
 * 		<li>To move compile-time profiles to the base class annotated by {@link
 * ActiveProfiles}. </li>
 *		<li>To create a custom additional class-level annotation and take it
 * into account in custom {@code ActiveProfilesResolver} implementation.
 * 		</li>
 * </ul>
 *
 * <p>{@code ActiveProfiles} instance created by framework is a plain <em>POJO</em>.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * @author Michail Nikolaev
 * @see ActiveProfiles
 * @see org.springframework.test.context.ActiveProfiles#resolver
 * @since 3.2.2
 */
public interface ActiveProfilesResolver {
	/**
	 * Resolves bean definition profiles to be used for test.
	 *
	 * @param testClass class of test
	 * @return profiles to use for loading {@code ApplicationContext} (never {@code
	 * null})
	 * @see ActiveProfiles#resolver
	 * @see ActiveProfiles#inheritProfiles
	 */
	String[] resolve(Class<?> testClass);
}
