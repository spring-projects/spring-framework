/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

public class DefaultSimpleBeanContract implements SimpleBeanContract {

	public SimpleBean anotherSimpleBean() {
		return new SimpleBean();
	}

}
