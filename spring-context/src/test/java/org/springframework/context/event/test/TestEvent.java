/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class TestEvent extends IdentifiableApplicationEvent {

	public final Object msg;

	public TestEvent(Object source, String id, String msg) {
		super(source, id);
		this.msg = msg;
	}

	public TestEvent(Object source, String msg) {
		super(source);
		this.msg = msg;
	}

	public TestEvent(Object source, Integer msg) {
		super(source);
		this.msg = msg;
	}

	public TestEvent(Object source) {
		this(source, "test");
	}

	public TestEvent() {
		this(new Object());
	}

}
