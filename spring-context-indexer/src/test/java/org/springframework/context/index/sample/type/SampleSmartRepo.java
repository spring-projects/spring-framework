/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.sample.type;

/**
 * A sample that implements both interface used to demonstrate that no
 * duplicate stereotypes are generated.
 *
 * @author Stephane Nicoll
 */
public class SampleSmartRepo
		implements SmartRepo<SampleEntity, Long>, Repo<SampleEntity, Long> {
}
