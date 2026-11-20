/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

/**
 * Marker interface implemented by all AOP proxies. Used to detect
 * whether objects are Spring-generated proxies.
 *
 * @author Rob Harrop
 * @since 2.0.1
 * @see org.springframework.aop.support.AopUtils#isAopProxy(Object)
 */
public interface SpringProxy {

}
