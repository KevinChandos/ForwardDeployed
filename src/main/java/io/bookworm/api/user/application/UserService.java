package io.bookworm.api.user.application;

import io.bookworm.api.user.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for member profile and delivery address management.
 * <p>
 * Why: Encapsulates member profile retrieval/updating, saved delivery addresses CRUD,
 * default address enforcement, and author follow/unfollow operations.
 */
public interface UserService {

    /**
     * Retrieves member profile and saved delivery addresses.
     */
    MemberProfileResponse getProfile(UUID memberId);

    /**
     * Updates member display profile.
     */
    MemberProfileResponse updateProfile(UUID memberId, UpdateProfileRequest request);

    /**
     * Retrieves all saved delivery addresses for a member.
     */
    List<MemberAddressDTO> getAddresses(UUID memberId);

    /**
     * Adds a new delivery address to the member's address book.
     */
    MemberAddressDTO addAddress(UUID memberId, CreateAddressRequest request);

    /**
     * Updates an existing delivery address in the member's address book.
     */
    MemberAddressDTO updateAddress(UUID memberId, UUID addressId, UpdateAddressRequest request);

    /**
     * Deletes a delivery address from the member's address book.
     */
    void deleteAddress(UUID memberId, UUID addressId);
}
