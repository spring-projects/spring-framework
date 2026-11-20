/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr16756;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
public class ScannedComponent {

	@Autowired
	private State state;

	public String iDoAnything() {
		return state.anyMethod();
	}


	public interface State {

		String anyMethod();
	}


	@Component
	@Scope(proxyMode = ScopedProxyMode.INTERFACES, value = "prototype")
	public static class StateImpl implements State {

		@Override
		public String anyMethod() {
			return "anyMethod called";
		}
	}

}
