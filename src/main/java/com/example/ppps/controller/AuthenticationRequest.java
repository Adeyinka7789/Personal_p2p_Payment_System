// REVIEW: full code review requested

package com.example.ppps.controller;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class AuthenticationRequest {

    @NotBlank(message = "Phone number is mandatory for login")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format. Must start with '+' and contain 1 to 15 digits.")
    private String phoneNumber;

    @NotBlank(message = "PIN is mandatory for login")
    @Pattern(regexp = "^\\d{6}$", message = "PIN must be exactly 6 digits")
    private String pin;
}
