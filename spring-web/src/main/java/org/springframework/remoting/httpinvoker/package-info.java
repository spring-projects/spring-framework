/**
 * Remoting classes for transparent Java-to-Java remoting via HTTP invokers.
 * Uses Java serialization just like RMI, but provides the same ease of setup
 * as Caucho's HTTP-based Hessian protocol.
 *
 * <p><b>HTTP invoker is the recommended protocol for Java-to-Java remoting.</b>
 * It is more powerful and more extensible than Hessian, at the expense of
 * being tied to Java. Nevertheless, it is as easy to set up as Hessian,
 * which is its main advantage compared to RMI.
 */
package org.springframework.remoting.httpinvoker;
