/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

/**
 * @author Juergen Hoeller
 */
public class StubTaskExecutor implements TaskExecutor {

	@Override
	public void execute(Runnable task) {
	}

}
