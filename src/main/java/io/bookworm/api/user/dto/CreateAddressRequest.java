package io.bookworm.api.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a new member address.
 * <p>
 * Why: Encapsulates all required address fields along with user label and default selection flag.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

    @NotBlank(message = "Address label is required (e.g. Home, Office)")
    @Size(max = 50, message = "Label cannot exceed 50 characters")
    private String label;

    @NotNull(message = "isDefault flag is required")
    private Boolean isDefault;

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
