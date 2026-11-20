/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

import org.springframework.beans.testfixture.beans.TestBean;

/**
 * @author Juergen Hoeller
 */
public interface TestBeanAwareMessenger extends ConfigurableMessenger {

	TestBean getTestBean();

	void setTestBean(TestBean testBean);

}
