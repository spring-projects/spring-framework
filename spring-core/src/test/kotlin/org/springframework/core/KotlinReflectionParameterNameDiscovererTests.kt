/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

/**
 * Tests for [KotlinReflectionParameterNameDiscoverer].
 *
 * @author Sebastien Deleuze
 */
class KotlinReflectionParameterNameDiscovererTests :
	AbstractReflectionParameterNameDiscovererKotlinTests(KotlinReflectionParameterNameDiscoverer())
