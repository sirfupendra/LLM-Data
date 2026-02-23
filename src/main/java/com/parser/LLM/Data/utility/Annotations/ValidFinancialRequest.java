package com.parser.LLM.Data.utility.Annotations;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FinancialRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFinancialRequest {

    String message() default "Either jsonInput or jsonArray must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
