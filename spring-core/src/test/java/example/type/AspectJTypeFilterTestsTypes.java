/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
