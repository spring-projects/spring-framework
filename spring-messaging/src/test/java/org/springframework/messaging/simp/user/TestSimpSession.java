/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * @author Rossen Stoyanchev
 */
public class TestSimpSession implements SimpSession {

	private final String id;

	private TestSimpUser user;

	private final Set<SimpSubscription> subscriptions = new HashSet<>();


	public TestSimpSession(String id) {
		this.id = id;
	}


	@Override
	public String getId() {
		return id;
	}

	@Override
	public TestSimpUser getUser() {
		return user;
	}

	public void setUser(TestSimpUser user) {
		this.user = user;
	}

	@Override
	public Set<SimpSubscription> getSubscriptions() {
		return subscriptions;
	}

	public void addSubscriptions(TestSimpSubscription... subscriptions) {
		for (TestSimpSubscription subscription : subscriptions) {
			subscription.setSession(this);
			this.subscriptions.add(subscription);
		}
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || (obj instanceof SimpSession that && this.id.equals(that.getId())));
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String toString() {
		return "id=" + this.id + ", subscriptions=" + this.subscriptions;
	}

}
