/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.lifecyclemethods;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class PackagePrivateInitDestroyBean extends InitDestroyBean {

	@PostConstruct
	void postConstruct() {
		this.initMethods.add("PackagePrivateInitDestroyBean.postConstruct");
	}

	@PreDestroy
	void preDestroy() {
		this.destroyMethods.add("PackagePrivateInitDestroyBean.preDestroy");
	}

}
