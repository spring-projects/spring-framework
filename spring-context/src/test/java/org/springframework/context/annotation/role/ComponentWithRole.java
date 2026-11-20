/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.role;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Description;
import org.springframework.context.annotation.Role;
import org.springframework.stereotype.Component;

@Component("componentWithRole")
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Description("A Component with a role")
public class ComponentWithRole {
}
