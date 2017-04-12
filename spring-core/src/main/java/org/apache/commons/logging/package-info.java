/**
 * Spring's variant of the
 * <a href="http://commons.apache.org/logging">Commons Logging API</a>
 * (for internal use only).
 *
 * <p>{@link org.apache.commons.logging.Log} is an unmodified repackaging.
 * However, {@link org.apache.commons.logging.LogFactory} is a very different
 * implementation which is minimized and optimized for Spring's purposes,
 * detecting Log4J 2.x and SLF4J 1.7 in the framework classpath and falling
 * back to {@code java.util.logging}.
 *
 * <p>Note that this Commons Logging variant is only meant to be used for
 * framework logging purposes, both in the core framework and in extensions.
 * For applications, prefer direct use of Log4J or SLF4J or {@code java.util.logging}.
 */
package org.apache.commons.logging;
