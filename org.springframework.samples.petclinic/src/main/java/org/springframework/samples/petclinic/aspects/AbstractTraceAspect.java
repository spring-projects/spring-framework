package org.springframework.samples.petclinic.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Aspect to illustrate Spring-driven load-time weaving.
 *
 * @author Ramnivas Laddad
 * @since 2.5
 */
@Aspect
public abstract class AbstractTraceAspect {

	private static final Log logger = LogFactory.getLog(AbstractTraceAspect.class);
	
	@Pointcut
	public abstract void traced();
	
	@Before("traced()")
	public void trace(JoinPoint.StaticPart jpsp) {
		if (logger.isTraceEnabled()) {
			logger.trace("Entering " + jpsp.getSignature().toLongString());
		}
	}

}
