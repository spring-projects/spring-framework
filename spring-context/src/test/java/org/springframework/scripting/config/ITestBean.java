/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.config;

/**
 * @author Mark Fisher
 */
public interface ITestBean {

	boolean isInitialized();

	boolean isDestroyed();

	ITestBean getOtherBean();

}
