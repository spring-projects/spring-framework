/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.sample;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Candidate with no matching annotation.
 *
 * @author Stephane Nicoll
 */
@Scope("None")
@Qualifier("None")
public class SampleNone {
}
