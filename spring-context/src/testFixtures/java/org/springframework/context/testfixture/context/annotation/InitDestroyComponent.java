/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class InitDestroyComponent {

	public final List<String> events = new ArrayList<>();

	@PostConstruct
	public void init() {
		this.events.add("init");
	}

	public void customInit() {
		this.events.add("customInit");
	}

	@PreDestroy
	public void destroy() {
		this.events.add("destroy");
	}

	public void customDestroy() {
		this.events.add("customDestroy");
	}

}
