/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa;

import java.io.Serializable;

import jakarta.persistence.EntityManager;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class EntityManagerPointcut implements Pointcut, Serializable {

	@Override
	public ClassFilter getClassFilter() {
		return (EntityManager.class::isAssignableFrom);
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		return MethodMatcher.TRUE;
	}

}
