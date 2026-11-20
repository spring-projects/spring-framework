/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Oliver Gierke
 */
@Component
public class ClassWithComplexConstructor {

	private final Dependency dependency;

	@Autowired ClassWithComplexConstructor selfReference;

	@Autowired
	public ClassWithComplexConstructor(Dependency dependency) {
		Assert.notNull(dependency, "No Dependency bean injected");
		this.dependency = dependency;
	}

	public Dependency getDependency() {
		return this.dependency;
	}

	public void method() {
		Assert.state(this.selfReference != this && AopUtils.isCglibProxy(this.selfReference),
				"Self reference must be a CGLIB proxy");
		this.dependency.method();
	}

}
