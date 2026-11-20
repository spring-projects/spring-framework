/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.aspectj;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
public class ClassWithProtectedAnnotatedMember {

	public void doSomething() {
		doInTransaction();
	}

	@Transactional
	protected void doInTransaction() {}

}
