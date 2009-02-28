package org.springframework.config.java;

import java.util.List;


/** Marker interface */
//TODO: SJC-242 document
//TODO: SJC-242 rename
public interface Validator {
    boolean supports(Object object);
    
    void validate(Object object, List<UsageError> errors);
}
