/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

/**
 * A sample configuration.
 *
 * @author Stephane Nicoll
 */
public class SimpleBeanConfiguration {

	public SimpleBean simpleBean() {
		return new SimpleBean();
	}

}
