package io.bookworm.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Persisted member address entity representation DTO.
 * <p>
 * Why: Transports address identifier, address label, and default shipping address flag to the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberAddressDTO {
    private UUID addressId;
    private String label;
    private Boolean isDefault;
    private String firstName;
    private String lastName;
    private String line1;
    private String line2;
    private String city;
    private String pinCode;
    private String state;
    private String country;
    private String phone;
}
