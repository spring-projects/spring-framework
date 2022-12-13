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

package org.springframework.core.io.support;

/**
 * Used by {@link SpringFactoriesLoaderTests}.
 *
 * @author Andy Wilkinson
 */
class ConstructorArgsDummyFactory implements DummyFactory {

	private final String string;

	public ConstructorArgsDummyFactory(String string) {
		this(string, 0);
	}

	private ConstructorArgsDummyFactory(String string, int reasonCode) {
		this.string = string;
	}

	@Override
	public String getString() {
		return this.string;
	}

}
