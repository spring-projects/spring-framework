/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.litemode;

import jakarta.annotation.PostConstruct;

/**
 * Simple POJO that contains lifecycle callbacks.
 *
 * @author Sam Brannen
 * @since 3.2
 */
public class LifecycleBean {

	private final String name;

	private boolean initialized = false;


	public LifecycleBean(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@PostConstruct
	public void init() {
		initialized = true;
	}

	public boolean isInitialized() {
		return this.initialized;
	}

}
