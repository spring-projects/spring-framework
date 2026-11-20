/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;


/**
 * @author Juergen Hoeller
 * @since 15.03.2005
 */
public class CountingTestBean extends TestBean {

	public static int count = 0;

	public CountingTestBean() {
		count++;
	}

}
