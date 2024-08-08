/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.testfixture.beans.factory.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public abstract class DeprecatedInjectionSamples {

	@Deprecated
	public static class DeprecatedEnvironment {}

	@Deprecated
	public static class DeprecatedSample {

		@Autowired
		Environment environment;

	}

	public static class DeprecatedFieldInjectionPointSample {

		@Autowired
		@Deprecated
		Environment environment;

	}

	public static class DeprecatedFieldInjectionTypeSample {

		@Autowired
		DeprecatedEnvironment environment;
	}

	public static class DeprecatedPrivateFieldInjectionTypeSample {

		@Autowired
		private DeprecatedEnvironment environment;
	}

	public static class DeprecatedMethodInjectionPointSample {

		@Autowired
		@Deprecated
		void setEnvironment(Environment environment) {}
	}

	public static class DeprecatedMethodInjectionTypeSample {

		@Autowired
		void setEnvironment(DeprecatedEnvironment environment) {}
	}

	public static class DeprecatedPrivateMethodInjectionTypeSample {

		@Autowired
		private void setEnvironment(DeprecatedEnvironment environment) {}
	}

}
