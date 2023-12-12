package com.lxcecho.validator.two;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Service
public class MyValidation1 {

    @Autowired
    private Validator validator;

    public boolean validatorByUserOne(User user) {
        Set<ConstraintViolation<User>> validate = validator.validate(user);
        return validate.isEmpty();
    }

}
