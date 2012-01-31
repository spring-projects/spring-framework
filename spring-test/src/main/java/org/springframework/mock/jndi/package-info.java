
/**
 *
 * The simplest implementation of the JNDI SPI that could possibly work.
 * 
 * <p>Useful for setting up a simple JNDI environment for test suites
 * or stand-alone applications. If, for example, JDBC DataSources get bound to the
 * same JNDI names as within a Java EE container, both application code and
 * configuration can be reused without changes.
 *
 */
package org.springframework.mock.jndi;

