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

package org.springframework.aot.hint;

import java.util.Objects;

/**
 * Base {@link TypeReference} implementation that ensures consistent behaviour
 * for {@code equals()}, {@code hashCode()}, and {@code toString()} based on
 * the {@linkplain #getCanonicalName() canonical name}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public abstract class AbstractTypeReference implements TypeReference {

	@Override
	public int hashCode() {
		return Objects.hash(getCanonicalName());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypeReference otherReference)) {
			return false;
		}
		return getCanonicalName().equals(otherReference.getCanonicalName());
	}

	@Override
	public String toString() {
		return getCanonicalName();
	}

}
