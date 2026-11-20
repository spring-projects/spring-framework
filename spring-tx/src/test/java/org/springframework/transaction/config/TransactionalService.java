/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.config;

import java.io.Serializable;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class TransactionalService implements Serializable {

	@Transactional("synch")
	public void setSomething(String name) {
	}

	@Transactional("noSynch")
	public void doSomething() {
	}

}
