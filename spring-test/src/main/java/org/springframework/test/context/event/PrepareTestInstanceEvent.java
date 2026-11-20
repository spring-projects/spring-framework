/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import org.springframework.test.context.TestContext;

/**
 * {@link TestContextEvent} published by the {@link EventPublishingTestExecutionListener} when
 * {@link org.springframework.test.context.TestExecutionListener#prepareTestInstance(TestContext)}
 * is invoked.
 *
 * @author Frank Scheffler
 * @since 5.2
 * @see org.springframework.test.context.event.annotation.PrepareTestInstance @PrepareTestInstance
 */
@SuppressWarnings("serial")
public class PrepareTestInstanceEvent extends TestContextEvent {

	public PrepareTestInstanceEvent(TestContext source) {
		super(source);
	}

}
