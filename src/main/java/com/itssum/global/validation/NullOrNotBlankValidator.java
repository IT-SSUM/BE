package com.itssum.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** 서비스가 값을 strip() 해서 저장하므로 같은 기준(Character.isWhitespace)으로 비었는지 본다. */
public class NullOrNotBlankValidator implements ConstraintValidator<NullOrNotBlank, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || !value.toString().strip().isEmpty();
    }
}
