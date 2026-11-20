/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.annotation;

/**
 * @author Juergen Hoeller
 */
@ManagedResource("${domain}:name=testBean5")
public class FactoryCreatedAnnotationTestBean extends AnnotationTestBean {

}
