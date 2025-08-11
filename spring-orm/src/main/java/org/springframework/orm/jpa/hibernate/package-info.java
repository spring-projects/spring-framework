/**
 * Hibernate-specific support classes, integrated with JPA.
 *
 * <p>Contains Hibernate-specific setup options as an alternative to JPA bootstrapping,
 * primarily for use with Hibernate's native {@code SessionFactory#getCurrentSession()}
 * but potentially also for JPA repositories or mixed use of native Hibernate and JPA.
 *
 * <p>As of Spring Framework 7.0, this package supersedes {@code orm.hibernate5} -
 * now for use with Hibernate ORM 7.1+, tightly integrated with JPA.
 */
@NullMarked
package org.springframework.orm.jpa.hibernate;

import org.jspecify.annotations.NullMarked;
