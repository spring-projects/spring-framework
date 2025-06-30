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

package org.springframework.core.testfixture.security;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

/**
 * An implementation of {@link Principal} for testing.
 *
 * @author Rossen Stoyanchev
 */
public class TestPrincipal implements Principal {

	private final String name;

	public TestPrincipal(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TestPrincipal p)) {
			return false;
		}
		return this.name.equals(p.name);
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

}
