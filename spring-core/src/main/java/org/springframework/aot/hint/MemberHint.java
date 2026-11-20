/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.lang.reflect.Member;

/**
 * Base hint that describes the need for reflection on a {@link Member}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public abstract class MemberHint {

	private final String name;


	protected MemberHint(String name) {
		this.name = name;
	}

	/**
	 * Return the name of the member.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

}
