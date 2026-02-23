package com.parser.LLM.Data.utility.Annotations;

import com.parser.LLM.Data.dto.request.FinancialConvertRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FinancialRequestValidator
        implements ConstraintValidator<ValidFinancialRequest, FinancialConvertRequest> {

    @Override
    public boolean isValid(FinancialConvertRequest request,
                           ConstraintValidatorContext context) {

        if (request == null) return false;

        boolean hasJsonInput = request.getJsonInput() != null;
        boolean hasJsonArray = request.getJsonArray() != null
                && request.getJsonArray().size()>0;

        // Rule: at least one must be present
        return hasJsonInput ^ hasJsonArray;
    }
}
