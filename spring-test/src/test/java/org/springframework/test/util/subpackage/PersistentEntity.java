/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util.subpackage;

/**
 * Abstract base class for <em>persistent entities</em>; intended for use in
 * unit tests.
 *
 * @author Sam Brannen
 * @since 2.5
 */
public abstract class PersistentEntity {

	private long id;


	public long getId() {
		return this.id;
	}

	protected void setId(long id) {
		this.id = id;
	}

}
