package org.springframework.config.java.annotation;

import java.util.List;

import org.springframework.config.java.model.UsageError;

/** Marker interface */
//TODO: SJC-242 document
//TODO: SJC-242 rename
public interface Validator {
    boolean supports(Object object);
    
    void validate(Object object, List<UsageError> errors);
}
