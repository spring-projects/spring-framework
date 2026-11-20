/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

/**
 * Bean that changes state on a business invocation, so that
 * we can check whether it's been invoked
 * @author Rod Johnson
 */
public class SideEffectBean {

	private int count;

	public void setCount(int count) {
		this.count = count;
	}

	public int getCount() {
		return this.count;
	}

	public void doWork() {
		++count;
	}

}
