/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.listener.endpoint;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.ResourceAdapter;

import org.springframework.jca.StubActivationSpec;

/**
 * @author Juergen Hoeller
 */
public class StubJmsActivationSpecFactory implements JmsActivationSpecFactory {

	@Override
	public ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config) {
		return new StubActivationSpec();
	}

}
