// REVIEW: full code review requested

package com.example.ppps.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PinResetRequest {
    @NotBlank(message = "Old PIN is required")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    @Size(min = 4, max = 6, message = "New PIN must be 4-6 digits")
    @Pattern(regexp = "^\\d+$", message = "New PIN must contain only digits")
    private String newPin;
}