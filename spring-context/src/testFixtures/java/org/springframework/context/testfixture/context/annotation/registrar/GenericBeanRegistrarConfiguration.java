/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation.registrar;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.testfixture.beans.factory.GenericBeanRegistrar;

@Configuration
@Import(GenericBeanRegistrar.class)
public class GenericBeanRegistrarConfiguration {
}
