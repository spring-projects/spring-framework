/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import jakarta.annotation.Resource;

public class PackagePrivateMethodResourceSample {

	String one;

	@Resource
	void setOne(String one) {
		this.one = one;
	}

}
