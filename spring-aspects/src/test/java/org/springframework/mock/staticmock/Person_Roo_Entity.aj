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

package org.springframework.mock.staticmock;

privileged aspect Person_Roo_Entity {

	@javax.persistence.PersistenceContext
	transient javax.persistence.EntityManager Person.entityManager;

	@javax.persistence.Id
	@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.AUTO)
	@javax.persistence.Column(name = "id")
	private java.lang.Long Person.id;

	@javax.persistence.Version
	@javax.persistence.Column(name = "version")
	private java.lang.Integer Person.version;

	public java.lang.Long Person.getId() {
		return this.id;
	}

	public void Person.setId(java.lang.Long id) {
		this.id = id;
	}

	public java.lang.Integer Person.getVersion() {
		return this.version;
	}

	public void Person.setVersion(java.lang.Integer version) {
		this.version = version;
	}

	@org.springframework.transaction.annotation.Transactional
	public void Person.persist() {
		if (this.entityManager == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		this.entityManager.persist(this);
	}

	@org.springframework.transaction.annotation.Transactional
	public void Person.remove() {
		if (this.entityManager == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		this.entityManager.remove(this);
	}

	@org.springframework.transaction.annotation.Transactional
	public void Person.flush() {
		if (this.entityManager == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		this.entityManager.flush();
	}

	@org.springframework.transaction.annotation.Transactional
	public void Person.merge() {
		if (this.entityManager == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		Person merged = this.entityManager.merge(this);
		this.entityManager.flush();
		this.id = merged.getId();
	}

	public static long Person.countPeople() {
		javax.persistence.EntityManager em = new Person().entityManager;
		if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		return (Long) em.createQuery("select count(o) from Person o").getSingleResult();
	}

	public static java.util.List<Person> Person.findAllPeople() {
		javax.persistence.EntityManager em = new Person().entityManager;
		if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		return em.createQuery("select o from Person o").getResultList();
	}

	public static Person Person.findPerson(java.lang.Long id) {
		if (id == null) throw new IllegalArgumentException("An identifier is required to retrieve an instance of Person");
		javax.persistence.EntityManager em = new Person().entityManager;
		if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		return em.find(Person.class, id);
	}

	public static java.util.List<Person> Person.findPersonEntries(int firstResult, int maxResults) {
		javax.persistence.EntityManager em = new Person().entityManager;
		if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
		return em.createQuery("select o from Person o").setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
	}

}
