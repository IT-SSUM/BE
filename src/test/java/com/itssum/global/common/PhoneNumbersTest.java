package com.itssum.global.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void formatsMobileAreaCodeAndRepresentativeNumbers() {
        assertEquals("010-1234-5678", PhoneNumbers.format("01012345678"));
        assertEquals("011-123-4567", PhoneNumbers.format("0111234567"));
        assertEquals("02-123-4567", PhoneNumbers.format("021234567"));
        assertEquals("02-1234-5678", PhoneNumbers.format("02-1234-5678"));
        assertEquals("031-123-4567", PhoneNumbers.format("0311234567"));
        assertEquals("1588-1234", PhoneNumbers.format("15881234"));
    }

    @Test
    void regexesMatchSignUpRules() {
        assertTrue("010-1234-5678".matches(PhoneNumbers.MOBILE_REGEX));
        assertFalse("02-1234-5678".matches(PhoneNumbers.MOBILE_REGEX));
        assertTrue("02-1234-5678".matches(PhoneNumbers.CONTACT_REGEX));
        assertTrue("1588-1234".matches(PhoneNumbers.CONTACT_REGEX));
        assertFalse("1234".matches(PhoneNumbers.CONTACT_REGEX));
    }
}
