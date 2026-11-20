/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jsr250;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Sam Brannen
 * @since 3.2
 */
class LifecycleBean {

	private final Log logger = LogFactory.getLog(LifecycleBean.class);


	@PostConstruct
	public void init() {
		logger.info("initializing");
	}

	@PreDestroy
	public void destroy() {
		logger.info("destroying");
	}

}
