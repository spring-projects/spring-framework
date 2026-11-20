/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

/**
 * A simple counter for use in simple tests (for example, how many times an advice was executed)
 *
 * @author Ramnivas Laddad
 */
final class Counter implements ICounter {

	private int count;

	public Counter() {
	}

	@Override
	public void increment() {
		count++;
	}

	@Override
	public void decrement() {
		count--;
	}

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public void setCount(int counter) {
		this.count = counter;
	}

	@Override
	public void reset() {
		this.count = 0;
	}

}
