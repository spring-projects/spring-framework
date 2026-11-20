/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;

/**
 * {@link ParseState} entry representing a pointcut.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class PointcutEntry implements ParseState.Entry {

	private final String name;


	/**
	 * Create a new {@code PointcutEntry} instance.
	 * @param name the bean name of the pointcut
	 */
	public PointcutEntry(String name) {
		this.name = name;
	}


	@Override
	public String toString() {
		return "Pointcut '" + this.name + "'";
	}

}
