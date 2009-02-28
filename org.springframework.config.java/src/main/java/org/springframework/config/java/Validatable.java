package org.springframework.config.java;

import java.util.List;

/**
 * Indicates a type is able to be validated for errors.
 * 
 * @see Validator
 *
 * @author Chris Beams
 */
public interface Validatable {

    /**
     * Validates this object, adding any errors to the supplied list of <var>errors</var>.
     */
    public void validate(List<UsageError> errors);

}
