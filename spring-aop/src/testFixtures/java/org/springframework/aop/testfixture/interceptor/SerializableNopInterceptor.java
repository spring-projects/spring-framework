/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.testfixture.interceptor;

import java.io.Serializable;

/**
 * Subclass of NopInterceptor that is serializable and
 * can be used to test proxy serialization.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class SerializableNopInterceptor extends NopInterceptor implements Serializable {

	/**
	 * We must override this field and the related methods as
	 * otherwise count won't be serialized from the non-serializable
	 * NopInterceptor superclass.
	 */
	private int count;

	@Override
	public int getCount() {
		return this.count;
	}

	@Override
	protected void increment() {
		++count;
	}

}
