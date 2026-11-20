/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.UUID;

/**
 * An {@link IdGenerator} that calls {@link java.util.UUID#randomUUID()}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1.5
 */
public class JdkIdGenerator implements IdGenerator {

	@Override
	public UUID generateId() {
		return UUID.randomUUID();
	}

}
