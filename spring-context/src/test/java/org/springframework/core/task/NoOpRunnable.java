/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

/**
 * A no-op {@link Runnable} implementation.
 *
 * @author Rick Evans
 */
public class NoOpRunnable implements Runnable {

	@Override
	public void run() {
		// explicit no-op
	}

}
