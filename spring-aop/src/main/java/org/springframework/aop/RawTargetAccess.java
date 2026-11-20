/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

/**
 * Marker for AOP proxy interfaces (in particular: introduction interfaces)
 * that explicitly intend to return the raw target object (which would normally
 * get replaced with the proxy object when returned from a method invocation).
 *
 * <p>Note that this is a marker interface in the style of {@link java.io.Serializable},
 * semantically applying to a declared interface rather than to the full class
 * of a concrete object. In other words, this marker applies to a particular
 * interface only (typically an introduction interface that does not serve
 * as the primary interface of an AOP proxy), and hence does not affect
 * other interfaces that a concrete AOP proxy may implement.
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 * @see org.springframework.aop.scope.ScopedObject
 */
public interface RawTargetAccess {

}
