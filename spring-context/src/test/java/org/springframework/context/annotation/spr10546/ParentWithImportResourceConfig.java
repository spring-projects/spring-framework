/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr10546;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 *
 * @author Rob Winch
 */
@Configuration
@ImportResource("classpath:org/springframework/context/annotation/spr10546/importedResource.xml")
public class ParentWithImportResourceConfig {

}
