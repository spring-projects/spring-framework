/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 10.06.2003
 */
public class BooleanTestBean {

	private boolean bool1;

	private Boolean bool2;

	public boolean isBool1() {
		return bool1;
	}

	public void setBool1(boolean bool1) {
		this.bool1 = bool1;
	}

	public Boolean getBool2() {
		return bool2;
	}

	public void setBool2(Boolean bool2) {
		this.bool2 = bool2;
	}

}
