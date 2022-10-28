/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.orm.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * Person entity listener.
 *
 * @author Sam Brannen
 * @since 5.3.18
 */
public class PersonListener {

	public static final List<String> methodsInvoked = new ArrayList<>();


	@PostLoad
	public void postLoad(Person person) {
		methodsInvoked.add("@PostLoad: " + person.getName());
	}

	@PrePersist
	public void prePersist(Person person) {
		methodsInvoked.add("@PrePersist: " + person.getName());
	}

	@PostPersist
	public void postPersist(Person person) {
		methodsInvoked.add("@PostPersist: " + person.getName());
	}

	@PreUpdate
	public void preUpdate(Person person) {
		methodsInvoked.add("@PreUpdate: " + person.getName());
	}

	@PostUpdate
	public void postUpdate(Person person) {
		methodsInvoked.add("@PostUpdate: " + person.getName());
	}

	@PreRemove
	public void preRemove(Person person) {
		methodsInvoked.add("@PreRemove: " + person.getName());
	}

	@PostRemove
	public void postRemove(Person person) {
		methodsInvoked.add("@PostRemove: " + person.getName());
	}

}
