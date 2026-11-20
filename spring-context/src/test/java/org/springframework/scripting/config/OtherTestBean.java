/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.config;

/**
 * @author Mark Fisher
 */
public class OtherTestBean implements ITestBean {

	@Override
	public ITestBean getOtherBean() {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return false;
	}

	@Override
	public boolean isDestroyed() {
		return false;
	}

}
