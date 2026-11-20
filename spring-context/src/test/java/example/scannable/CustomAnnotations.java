/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package example.scannable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CustomAnnotations {

	@Retention(RetentionPolicy.RUNTIME)
	private @interface PrivateAnnotation {
		String value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@PrivateAnnotation("special")
	public @interface SpecialAnnotation {
	}

}
