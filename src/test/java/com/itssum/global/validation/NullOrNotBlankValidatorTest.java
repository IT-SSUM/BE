package com.itssum.global.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NullOrNotBlankValidatorTest {

    private static final String FULL_WIDTH_SPACE = String.valueOf((char) 0x3000);

    private final NullOrNotBlankValidator validator = new NullOrNotBlankValidator();

    @Test
    void nullMeansUnchangedAndPasses() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void textWithLettersPasses() {
        assertTrue(validator.isValid(" 최윤서 ", null));
    }

    @Test
    void emptyOrWhitespaceOnlyFailsIncludingFullWidthSpace() {
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("   ", null));
        assertFalse(validator.isValid("\t", null));
        assertFalse(validator.isValid(FULL_WIDTH_SPACE + FULL_WIDTH_SPACE, null));
    }
}
