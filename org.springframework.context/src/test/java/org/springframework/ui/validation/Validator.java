package org.springframework.ui.validation;

import java.util.List;

public interface Validator {

	ValidateResults validate(Object model, List<String> properties);

}
