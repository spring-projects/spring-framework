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

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with {@link ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @see AnnotationTypeFilterTests
 */
class AnnotationTypeFilterTestsTypes {

	@AnnotationTypeFilterTests.InheritedAnnotation
	private static class SomeComponent {
	}


	@AnnotationTypeFilterTests.InheritedAnnotation
	private interface SomeComponentInterface {
	}


	@SuppressWarnings("unused")
	private static class SomeClassWithSomeComponentInterface implements Cloneable, SomeComponentInterface {
	}


	@SuppressWarnings("unused")
	private static class SomeSubclassOfSomeComponent extends SomeComponent {
	}

	@AnnotationTypeFilterTests.NonInheritedAnnotation
	private static class SomeClassMarkedWithNonInheritedAnnotation {
	}


	@SuppressWarnings("unused")
	private static class SomeSubclassOfSomeClassMarkedWithNonInheritedAnnotation extends SomeClassMarkedWithNonInheritedAnnotation {
	}


	@SuppressWarnings("unused")
	private static class SomeNonCandidateClass {
	}

}
