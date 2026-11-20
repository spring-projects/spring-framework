/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.springframework.context.Lifecycle;

/**
 * @author Mark Fisher
 */
public class LifecycleTestBean implements Lifecycle {

	private static int startCounter;

	private static int stopCounter;


	private int startOrder;

	private int stopOrder;

	private boolean running;


	public int getStartOrder() {
		return startOrder;
	}

	public int getStopOrder() {
		return stopOrder;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void start() {
		this.startOrder = ++startCounter;
		this.running = true;
	}

	@Override
	public void stop() {
		this.stopOrder = ++stopCounter;
		this.running = false;
	}

}
