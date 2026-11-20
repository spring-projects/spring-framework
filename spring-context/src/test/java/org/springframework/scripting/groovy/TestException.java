/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

/**
 * @author Dave Syer
 *
 */
@SuppressWarnings("serial")
public class TestException extends RuntimeException {

	public TestException(String string) {
		super(string);
	}

}
