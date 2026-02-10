package net.devstudy.resume.shared.validation.validator;

import java.util.regex.Pattern;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.RestoreIdentifier;

public class RestoreIdentifierConstraintValidator implements ConstraintValidator<RestoreIdentifier, String> {

    private static final Pattern UID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return EMAIL_PATTERN.matcher(trimmed).matches();
        }
        if (UID_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        return isValidPhone(trimmed);
    }

    private boolean isValidPhone(String value) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(value, "ZZ");
            return phoneNumberUtil.isValidNumber(number);
        } catch (NumberParseException ex) {
            return false;
        }
    }
}
