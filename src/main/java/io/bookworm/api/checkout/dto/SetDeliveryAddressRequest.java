package io.bookworm.api.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for setting the delivery address on a checkout session.
 * <p>
 * Why: Accepts either an existing saved addressId or manual address details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetDeliveryAddressRequest {

    private UUID addressId;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Line 1 is required")
    @Size(max = 250, message = "Line 1 cannot exceed 250 characters")
    private String line1;

    @Size(max = 250, message = "Line 2 cannot exceed 250 characters")
    private String line2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "Pin code is required")
    @Size(max = 20, message = "Pin code cannot exceed 20 characters")
    private String pinCode;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @NotBlank(message = "Country is required")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be an ISO-3166 alpha-2 2-letter uppercase code")
    private String country;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone must be in E.164 format")
    private String phone;
}
