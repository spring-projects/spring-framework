/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PassThroughSourceExtractor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class PassThroughSourceExtractorTests {

	@Test
	void passThroughContract() {
		Object source = new Object();
		Object extractedSource = new PassThroughSourceExtractor().extractSource(source, null);
		assertThat(extractedSource).as("The contract of PassThroughSourceExtractor states that the supplied " +
				"source object *must* be returned as-is").isSameAs(source);
	}

	@Test
	void passThroughContractEvenWithNull() {
		Object extractedSource = new PassThroughSourceExtractor().extractSource(null, null);
		assertThat(extractedSource).as("The contract of PassThroughSourceExtractor states that the supplied " +
				"source object *must* be returned as-is (even if null)").isNull();
	}

}
