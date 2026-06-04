package vn.amela.leaveservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import vn.amela.leaveservice.dto.request.CreateLeaveRequest;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, CreateLeaveRequest> {

    @Override
    public boolean isValid(CreateLeaveRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.fromDate() == null || request.toDate() == null) {
            return true;
        }
        return !request.fromDate().isAfter(request.toDate());
    }
}
