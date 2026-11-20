/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
