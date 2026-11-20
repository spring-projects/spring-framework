/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.sample;

import org.springframework.stereotype.Component;

/**
 * Candidate with an inner class that isn't static (and should therefore not be added).
 *
 * @author Stephane Nicoll
 */
public class SampleNonStaticEmbedded {

	@Component
	public class InvalidCandidate {

	}

}
