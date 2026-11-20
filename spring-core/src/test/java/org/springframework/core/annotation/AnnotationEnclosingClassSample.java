/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Example class used to test {@link AnnotationsScanner} with enclosing classes.
 *
 * @author Phillip Webb
 * @since 5.2
 */
@AnnotationEnclosingClassSample.EnclosedOne
public class AnnotationEnclosingClassSample {

	@EnclosedTwo
	public static class EnclosedStatic {

		@EnclosedThree
		public static class EnclosedStaticStatic {
		}

	}

	@EnclosedTwo
	public class EnclosedInner {

		@EnclosedThree
		public class EnclosedInnerInner {
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface EnclosedOne {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface EnclosedTwo {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface EnclosedThree {
	}

}
