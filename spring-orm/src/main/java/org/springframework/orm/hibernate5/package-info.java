/**
 * Package providing integration of
 * <a href="https://hibernate.org/">Hibernate 5.x</a>
 * with Spring concepts.
 *
 * <p>Contains an implementation of Spring's transaction SPI for local Hibernate transactions.
 * This package is intentionally rather minimal, with no template classes or the like,
 * in order to follow Hibernate recommendations as closely as possible. We recommend
 * using Hibernate's native <code>sessionFactory.getCurrentSession()</code> style.
 *
 * <p><b>This package supports Hibernate 5.x only.</b>
 */
@NonNullApi
@NonNullFields
package org.springframework.orm.hibernate5;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
