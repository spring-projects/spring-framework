/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.orm.jpa.domain;

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
