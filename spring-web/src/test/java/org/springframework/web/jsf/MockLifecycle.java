/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.jsf;

import jakarta.faces.FacesException;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseListener;
import jakarta.faces.lifecycle.Lifecycle;

/**
 * @author Juergen Hoeller
 * @since 29.01.2006
 */
public class MockLifecycle extends Lifecycle {

	@Override
	public void addPhaseListener(PhaseListener phaseListener) {
	}

	@Override
	public void execute(FacesContext facesContext) throws FacesException {
	}

	@Override
	public PhaseListener[] getPhaseListeners() {
		return null;
	}

	@Override
	public void removePhaseListener(PhaseListener phaseListener) {
	}

	@Override
	public void render(FacesContext facesContext) throws FacesException {
	}

}
