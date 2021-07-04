/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.simp.user;

import org.springframework.util.ObjectUtils;

/**
 * @author Rossen Stoyanchev
 */
public class TestSimpSubscription implements SimpSubscription {

	private final String destination;

	private final String id;

	private TestSimpSession session;


	public TestSimpSubscription(String id, String destination) {
		this.destination = destination;
		this.id = id;
	}


	@Override
	public String getId() {
		return id;
	}

	@Override
	public TestSimpSession getSession() {
		return this.session;
	}

	public void setSession(TestSimpSession session) {
		this.session = session;
	}

	@Override
	public String getDestination() {
		return destination;
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SimpSubscription)) {
			return false;
		}
		SimpSubscription otherSubscription = (SimpSubscription) other;
		return (ObjectUtils.nullSafeEquals(getSession(), otherSubscription.getSession()) &&
				this.id.equals(otherSubscription.getId()));
	}

	@Override
	public int hashCode() {
		return this.id.hashCode() * 31 + ObjectUtils.nullSafeHashCode(getSession());
	}

	@Override
	public String toString() {
		return "destination=" + this.destination;
	}

}
