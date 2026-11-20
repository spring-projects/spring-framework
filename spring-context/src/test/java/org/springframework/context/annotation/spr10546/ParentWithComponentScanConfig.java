/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation.spr10546;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.spr10546.scanpackage.AEnclosingConfig;

/**
 *
 * @author Rob Winch
 */
@Configuration
@ComponentScan(basePackageClasses=AEnclosingConfig.class)
public class ParentWithComponentScanConfig {

}
