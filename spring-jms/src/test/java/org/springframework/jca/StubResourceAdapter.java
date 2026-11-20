/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jca;

import javax.transaction.xa.XAResource;

import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

/**
 * @author Juergen Hoeller
 */
public class StubResourceAdapter implements ResourceAdapter {

	@Override
	public void start(BootstrapContext bootstrapContext) {
	}

	@Override
	public void stop() {
	}

	@Override
	public void endpointActivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory messageEndpointFactory, ActivationSpec activationSpec) {
	}

	@Override
	public XAResource[] getXAResources(ActivationSpec[] activationSpecs) {
		return null;
	}

}
