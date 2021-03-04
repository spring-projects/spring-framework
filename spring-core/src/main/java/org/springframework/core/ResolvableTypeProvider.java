/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Any object can implement this interface to provide its actual {@link ResolvableType}.
 *
 * <p>Such information is very useful when figuring out if the instance matches a generic
 * signature as Java does not convey the signature at runtime.
 *
 * <p>Users of this interface should be careful in complex hierarchy scenarios, especially
 * when the generic type signature of the class changes in sub-classes. It is always
 * possible to return {@code null} to fallback on a default behavior.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
public interface ResolvableTypeProvider {

	/**
	 * Return the {@link ResolvableType} describing this instance
	 * (or {@code null} if some sort of default should be applied instead).
	 */
	@Nullable
	ResolvableType getResolvableType();

}
