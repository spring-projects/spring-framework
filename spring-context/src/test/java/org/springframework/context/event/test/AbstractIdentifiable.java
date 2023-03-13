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

package org.springframework.context.event.test;

import java.util.UUID;

import org.springframework.lang.Nullable;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractIdentifiable implements Identifiable {

	private final String id;

	public AbstractIdentifiable() {
		this.id = UUID.randomUUID().toString();
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		AbstractIdentifiable that = (AbstractIdentifiable) o;

		return this.id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}
