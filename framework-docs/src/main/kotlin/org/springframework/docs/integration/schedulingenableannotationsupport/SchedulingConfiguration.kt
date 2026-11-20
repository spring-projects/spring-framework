/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.schedulingenableannotationsupport

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

// tag::snippet[]
@Configuration
@EnableAsync
@EnableScheduling
class SchedulingConfiguration
// end::snippet[]