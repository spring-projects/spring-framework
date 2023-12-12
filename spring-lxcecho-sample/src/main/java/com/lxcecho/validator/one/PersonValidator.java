package com.lxcecho.validator.one;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class PersonValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Person.class.equals(clazz);
    }

    //校验规则
    @Override
    public void validate(Object target, Errors errors) {
        //name不能为空
        ValidationUtils.rejectIfEmpty(errors,
                "name", "name.empty","name is null");

        //age 不能小于0，不能大于200
        Person p = (Person)target;
        if(p.getAge() < 0) {
            errors.rejectValue("age","age.value.error","age < 0");
        } else if(p.getAge() > 200) {
            errors.rejectValue("age","age.value.error.old","age > 200");
        }
    }
}
