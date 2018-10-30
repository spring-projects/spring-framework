/**
 * Spring's variant of the
 * <a href="http://commons.apache.org/logging">Commons Logging API</a>:
 * with special support for Log4J 2, SLF4J and {@code java.util.logging}.
 *
 * <p>This {@code impl} package is only present for binary compatibility
 * with existing Commons Logging usage, e.g. in Commons Configuration.
 * {@code NoOpLog} can be used as a {@code Log} fallback instance, and
 * {@code SimpleLog} is not meant to work (issuing a warning when used).
 */
package org.apache.commons.logging.impl;
