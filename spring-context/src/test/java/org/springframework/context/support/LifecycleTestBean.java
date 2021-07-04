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
