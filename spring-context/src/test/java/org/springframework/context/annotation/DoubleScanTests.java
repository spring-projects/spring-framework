/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class DoubleScanTests extends SimpleScanTests {

	@Override
	protected String[] getConfigLocations() {
		return new String[] {"doubleScanTests.xml"};
	}

}
