/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

/**
 * @author Ramnivas Laddad
 */
interface ICounter {

	void increment();

	void decrement();

	int getCount();

	void setCount(int counter);

	void reset();

}
