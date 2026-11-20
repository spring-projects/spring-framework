/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.configuration.a;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.configuration.Bar;

public abstract class BaseConfig {

	// ---- reproduce ----
	@Bean
	Bar packagePrivateBar() {
		return new Bar();
	}

	public Bar reproBar() {
		return packagePrivateBar();
	}

	// ---- workaround ----
	@Bean
	protected Bar protectedBar() {
		return new Bar();
	}

	public Bar workaroundBar() {
		return protectedBar();
	}
}
