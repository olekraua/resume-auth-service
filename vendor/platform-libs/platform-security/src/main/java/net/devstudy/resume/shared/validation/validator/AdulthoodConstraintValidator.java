package net.devstudy.resume.shared.validation.validator;

import java.sql.Date;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.devstudy.resume.shared.validation.annotation.Adulthood;

/**
 * Перевіряє, що дата народження вказує на вік не менше adulthoodAge.
 */
public class AdulthoodConstraintValidator implements ConstraintValidator<Adulthood, Date> {
    private int adulthoodAge;

    @Override
    public void initialize(Adulthood constraintAnnotation) {
        this.adulthoodAge = constraintAnnotation.adulthoodAge();
    }

    @Override
    public boolean isValid(Date value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate critical = LocalDate.now().minusYears(adulthoodAge);
        return value.toLocalDate().isBefore(critical) || value.toLocalDate().isEqual(critical);
    }
}
