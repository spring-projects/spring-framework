
/**
 *
 * Package providing integration of
 * <a href="http://www.hibernate.org">Hibernate 4.0</a>
 * with Spring concepts.
 * 
 * <p>Contains an implementation of Spring's transaction SPI for local Hibernate transactions.
 * This package is intentionally rather minimal, relying on native Hibernate builder APIs
 * for building a SessionFactory (for example in an @Bean method in a @Configuration class).
 *
 * <p><b>This package supports Hibernate 4.x only.</b>
 * See the org.springframework.orm.hibernate3 package for Hibernate 3.x support.
 *
 */
package org.springframework.orm.hibernate4;

