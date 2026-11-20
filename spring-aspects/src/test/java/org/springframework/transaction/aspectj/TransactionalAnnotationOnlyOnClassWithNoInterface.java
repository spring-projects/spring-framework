/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.aspectj;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TransactionalAnnotationOnlyOnClassWithNoInterface {

	public Object echo(Throwable t) throws Throwable {
		if (t != null) {
			throw t;
		}
		return t;
	}

	void nonTransactionalMethod() {
		// no-op
	}

}

