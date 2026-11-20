/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * {@link ParseState} entry representing an advice element.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class AdviceEntry implements ParseState.Entry {

	private final String kind;


	/**
	 * Create a new {@code AdviceEntry} instance.
	 * @param kind the kind of advice represented by this entry (before, after, around)
	 */
	public AdviceEntry(String kind) {
		this.kind = kind;
	}


	@Override
	public String toString() {
		return "Advice (" + this.kind + ")";
	}

}
