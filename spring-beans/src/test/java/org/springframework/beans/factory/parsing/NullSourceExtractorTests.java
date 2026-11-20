/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
class NullSourceExtractorTests {

	@Test
	void passThroughContract() {
		Object source = new Object();
		Object extractedSource = new NullSourceExtractor().extractSource(source, null);
		assertThat(extractedSource).as("The contract of NullSourceExtractor states that the extraction *always* return null").isNull();
	}

	@Test
	void passThroughContractEvenWithNull() {
		Object extractedSource = new NullSourceExtractor().extractSource(null, null);
		assertThat(extractedSource).as("The contract of NullSourceExtractor states that the extraction *always* return null").isNull();
	}

}
