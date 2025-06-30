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

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * {@link ParameterNameDiscoverer} implementation that tries several discoverer
 * delegates in succession. Those added first in the {@code addDiscoverer} method
 * have the highest priority. If one returns {@code null}, the next will be tried.
 *
 * <p>The default behavior is to return {@code null} if no discoverer matches.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
public class PrioritizedParameterNameDiscoverer implements ParameterNameDiscoverer {

	private final List<ParameterNameDiscoverer> parameterNameDiscoverers = new ArrayList<>(2);


	/**
	 * Add a further {@link ParameterNameDiscoverer} delegate to the list of
	 * discoverers that this {@code PrioritizedParameterNameDiscoverer} checks.
	 */
	public void addDiscoverer(ParameterNameDiscoverer pnd) {
		this.parameterNameDiscoverers.add(pnd);
	}


	@Override
	public @Nullable String @Nullable [] getParameterNames(Method method) {
		for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
			@Nullable String[] result = pnd.getParameterNames(method);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	@Override
	public @Nullable String @Nullable [] getParameterNames(Constructor<?> ctor) {
		for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
			@Nullable String[] result = pnd.getParameterNames(ctor);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

}
