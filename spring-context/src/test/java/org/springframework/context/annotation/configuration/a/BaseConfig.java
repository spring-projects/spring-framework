/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.context.annotation.configuration.a;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.configuration.PackagePrivateBeanMethodInheritanceTests.Bar;

public abstract class BaseConfig {

	// ---- reproduce ----
	@Bean
	Bar packagePrivateBar() {
		return new Bar();
	}

	public Bar reproBar() {
		return packagePrivateBar();
	}

	// ---- workaround ----
	@Bean
	protected Bar protectedBar() {
		return new Bar();
	}

	public Bar workaroundBar() {
		return protectedBar();
	}
}