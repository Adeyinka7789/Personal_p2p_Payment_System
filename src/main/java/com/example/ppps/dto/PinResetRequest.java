package com.example.ppps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PinResetRequest {
    @NotBlank(message = "Current PIN is required")
    @Pattern(regexp = "^\\d+$", message = "Current PIN must contain only digits")
    private String currentPin;

    @NotBlank(message = "New PIN is required")
    @Size(min = 4, max = 6, message = "New PIN must be 4-6 digits")
    @Pattern(regexp = "^\\d+$", message = "New PIN must contain only digits")
    private String newPin;

    @NotBlank(message = "Confirm PIN is required")
    @Size(min = 4, max = 6, message = "Confirm PIN must be 4-6 digits")
    @Pattern(regexp = "^\\d+$", message = "Confirm PIN must contain only digits")
    private String confirmPin;
}