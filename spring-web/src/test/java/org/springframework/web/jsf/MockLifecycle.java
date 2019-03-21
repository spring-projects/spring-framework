/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.jsf;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

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