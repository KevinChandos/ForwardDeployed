package io.bookworm.api.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for updating an existing member address.
 * <p>
 * Why: Supports optional/partial updates to existing saved addresses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {

    @Size(max = 50, message = "Label cannot exceed 50 characters")
    private String label;

    private Boolean isDefault;

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 250, message = "Line 1 cannot exceed 250 characters")
    private String line1;

    @Size(max = 250, message = "Line 2 cannot exceed 250 characters")
    private String line2;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 20, message = "Pin code cannot exceed 20 characters")
    private String pinCode;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be an ISO-3166 alpha-2 2-letter uppercase code")
    private String country;

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone must be in E.164 format")
    private String phone;
}
