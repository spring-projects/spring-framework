/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

/**
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public class AnotherTestEvent extends IdentifiableApplicationEvent {

	public final Object content;

	public AnotherTestEvent(Object source, Object content) {
		super(source);
		this.content = content;
	}

}
