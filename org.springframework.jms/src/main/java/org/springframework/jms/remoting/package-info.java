
/**
 *
 * Remoting classes for transparent Java-to-Java remoting via a JMS provider.
 * 
 * <p>Allows the target service to be load-balanced across a number of queue
 * receivers, and provides a level of indirection between the client and the
 * service: They only need to agree on a queue name and a service interface.
 *
 */
package org.springframework.jms.remoting;

