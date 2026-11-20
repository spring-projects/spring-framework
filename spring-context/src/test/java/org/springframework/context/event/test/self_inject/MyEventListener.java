/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test.self_inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyEventListener implements ApplicationListener<MyEvent> {

	public int eventCount;

	@SuppressWarnings("unused")
	@Autowired
	private MyEventListener eventDemoListener;

	@Override
	public void onApplicationEvent(MyEvent event) {
		eventCount++;
	}

}
