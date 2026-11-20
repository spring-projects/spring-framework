/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

/**
 * @author Adrian Colyer
 * @since 2.0
 */
interface AnnotatedTestBean {

	String doThis();

	String doThat();

	String doTheOther();

	String[] doArray();

}
