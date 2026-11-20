/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.ApplicationContext;

/**
 * @author Juergen Hoeller
 * @since 08.08.2006
 */
public interface ContextScriptBean extends ScriptBean {

	TestBean getTestBean();

	ApplicationContext getApplicationContext();

}
