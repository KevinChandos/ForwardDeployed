package io.bookworm.api.user.dto;

import io.bookworm.api.auth.dto.MemberSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Detailed member profile response DTO.
 * <p>
 * Why: Provides full member personal information alongside saved address book entries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfileResponse {
    private MemberSummaryDTO member;
    private String phoneNumber;
    private List<MemberAddressDTO> addresses;
}
