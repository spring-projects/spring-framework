/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web.portlet;

import java.io.Serializable;
import javax.portlet.Event;
import javax.xml.namespace.QName;

/**
 * Mock implementation of the {@link javax.portlet.Event} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MockEventRequest
 */
public class MockEvent implements Event {

	private final QName name;

	private final Serializable value;


	/**
	 * Create a new MockEvent with the given name.
	 * @param name the name of the event
	 */
	public MockEvent(QName name) {
		this.name = name;
		this.value = null;
	}

	/**
	 * Create a new MockEvent with the given name and value.
	 * @param name the name of the event
	 * @param value the associated payload of the event
	 */
	public MockEvent(QName name, Serializable value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Create a new MockEvent with the given name.
	 * @param name the name of the event
	 */
	public MockEvent(String name) {
		this.name = new QName(name);
		this.value = null;
	}

	/**
	 * Create a new MockEvent with the given name and value.
	 * @param name the name of the event
	 * @param value the associated payload of the event
	 */
	public MockEvent(String name, Serializable value) {
		this.name = new QName(name);
		this.value = value;
	}


	@Override
	public QName getQName() {
		return this.name;
	}

	@Override
	public String getName() {
		return this.name.getLocalPart();
	}

	@Override
	public Serializable getValue() {
		return this.value;
	}

}