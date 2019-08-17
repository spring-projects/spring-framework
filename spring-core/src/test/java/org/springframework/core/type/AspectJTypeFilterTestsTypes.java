/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.type;

import org.springframework.stereotype.Component;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with {@link ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @see AspectJTypeFilterTests
 */
public class AspectJTypeFilterTestsTypes {

	interface SomeInterface {
	}

	static class SomeClass {
	}

	static class SomeClassExtendingSomeClass extends SomeClass {
	}

	static class SomeClassImplementingSomeInterface implements SomeInterface {
	}

	static class SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface
			extends SomeClassExtendingSomeClass implements SomeInterface {
	}

	@Component
	static class SomeClassAnnotatedWithComponent {
	}

}
