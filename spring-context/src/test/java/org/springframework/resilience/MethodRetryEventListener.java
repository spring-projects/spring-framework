/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.resilience;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;
import org.springframework.resilience.retry.MethodRetryEvent;

/**
 * @author Juergen Hoeller
 * @since 7.0.3
 */
class MethodRetryEventListener implements ApplicationListener<MethodRetryEvent> {

	public final List<MethodRetryEvent> events = new ArrayList<>();

	@Override
	public void onApplicationEvent(MethodRetryEvent event) {
		this.events.add(event);
	}

}
