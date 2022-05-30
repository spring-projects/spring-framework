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

/**
 * We must use a standalone set of types to ensure that no one else is loading
 * them and interfering with
 * {@link org.springframework.core.type.ClassloadingAssertions#assertClassNotLoaded(String)}.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Sam Brannen
 * @see org.springframework.core.type.AnnotationTypeFilterTests
 */
public class AnnotationTypeFilterTestsTypes {

	@InheritedAnnotation
	public static class SomeComponent {
	}


	@InheritedAnnotation
	public interface SomeComponentInterface {
	}


	@SuppressWarnings("unused")
	public static class SomeClassWithSomeComponentInterface implements Cloneable, SomeComponentInterface {
	}


	@SuppressWarnings("unused")
	public static class SomeSubclassOfSomeComponent extends SomeComponent {
	}

	@NonInheritedAnnotation
	public static class SomeClassMarkedWithNonInheritedAnnotation {
	}


	@SuppressWarnings("unused")
	public static class SomeSubclassOfSomeClassMarkedWithNonInheritedAnnotation extends SomeClassMarkedWithNonInheritedAnnotation {
	}


	@SuppressWarnings("unused")
	public static class SomeNonCandidateClass {
	}

}
