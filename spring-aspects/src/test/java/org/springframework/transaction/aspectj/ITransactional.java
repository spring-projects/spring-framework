/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.aspectj;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ITransactional {

	Object echo(Throwable t) throws Throwable;

}
