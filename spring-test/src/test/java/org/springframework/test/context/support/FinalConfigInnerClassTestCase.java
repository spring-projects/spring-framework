/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.springframework.context.annotation.Configuration;

/**
 * Not an actual <em>test case</em>.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see AnnotationConfigContextLoaderTests
 */
public class FinalConfigInnerClassTestCase {

	// Intentionally FINAL.
	@Configuration
	static final class Config {
	}

}
