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

package example.type;

import org.springframework.core.testfixture.stereotype.Component;

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link org.springframework.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Sam Brannen
 * @see org.springframework.core.type.AspectJTypeFilterTests
 */
public class AspectJTypeFilterTestsTypes {

	public interface SomeInterface {
	}

	public static class SomeClass {
	}

	public static class SomeClassExtendingSomeClass extends SomeClass {
	}

	public static class SomeClassImplementingSomeInterface implements SomeInterface {
	}

	public static class SomeClassExtendingSomeClassExtendingSomeClassAndImplementingSomeInterface
			extends SomeClassExtendingSomeClass implements SomeInterface {
	}

	@Component
	public static class SomeClassAnnotatedWithComponent {
	}

}
