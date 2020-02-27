/**
 * Spring's variant of the
 * <a href="https://commons.apache.org/logging">Commons Logging API</a>:
 * with special support for Log4J 2, SLF4J and {@code java.util.logging}.
 *
 * <p>This is a custom bridge along the lines of {@code jcl-over-slf4j}.
 * You may exclude {@code spring-jcl} and switch to {@code jcl-over-slf4j}
 * instead if you prefer the hard-bound SLF4J bridge. However, Spring's own
 * bridge provides a better out-of-the-box experience when using Log4J 2
 * or {@code java.util.logging}, with no extra bridge jars necessary, and
 * also easier setup of SLF4J with Logback (no JCL exclude, no JCL bridge).
 *
 * <p>{@link org.apache.commons.logging.Log} is equivalent to the original.
 * However, {@link org.apache.commons.logging.LogFactory} is a very different
 * implementation which is minimized and optimized for Spring's purposes,
 * detecting Log4J 2.x and SLF4J 1.7 in the framework classpath and falling
 * back to {@code java.util.logging}. If you run into any issues with this
 * implementation, consider excluding {@code spring-jcl} and switching to the
 * standard {@code commons-logging} artifact or to {@code jcl-over-slf4j}.
 *
 * <p>Note that this Commons Logging bridge is only meant to be used for
 * framework logging purposes, both in the core framework and in extensions.
 * For applications, prefer direct use of Log4J/SLF4J or {@code java.util.logging}.
 */
package org.apache.commons.logging;
