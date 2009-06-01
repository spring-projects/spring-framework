
/**
 *
 * SPI package allowing Spring AOP framework to handle arbitrary advice types.
 * <br>
 * Users who want merely to <i>use</i> the Spring AOP framework, rather than extend
 * its capabilities, don't need to concern themselves with this package.
 * <br>
 * 	You may wish to use these adapters to wrap Spring-specific advices, such as MethodBeforeAdvice,
 * 	in MethodInterceptor, to allow their use in another AOP framework supporting the AOP Alliance interfaces. 
 * </br>
 * <br>
 * 	These adapters do not depend on any other Spring framework classes to allow such usage.
 * </br>
 *
 */
package org.springframework.aop.framework.adapter;

