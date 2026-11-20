/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

/**
 * Showcase a factory method that is defined on an interface.
 *
 * @author Stephane Nicoll
 */
public interface SimpleBeanContract {

	default SimpleBean simpleBean() {
		return new SimpleBean();
	}

}
