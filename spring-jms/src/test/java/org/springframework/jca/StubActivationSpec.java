/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jca;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;

/**
 * @author Juergen Hoeller
 */
public class StubActivationSpec implements ActivationSpec {

	@Override
	public void validate() {
	}

	@Override
	public ResourceAdapter getResourceAdapter() {
		return null;
	}

	@Override
	public void setResourceAdapter(ResourceAdapter resourceAdapter) {
	}

}
