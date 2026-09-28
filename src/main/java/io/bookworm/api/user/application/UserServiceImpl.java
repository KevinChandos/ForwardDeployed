package io.bookworm.api.user.application;

import io.bookworm.api.auth.domain.Member;
import io.bookworm.api.auth.domain.MemberAddress;
import io.bookworm.api.auth.infrastructure.MemberRepository;
import io.bookworm.api.common.exception.BusinessRuleException;
import io.bookworm.api.common.exception.ResourceNotFoundException;
import io.bookworm.api.user.dto.*;
import io.bookworm.api.user.infrastructure.MemberAddressRepository;
import io.bookworm.api.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for User & Profile bounded context.
 * <p>
 * Why: Orchestrates member profile maintenance, address book operations, default address
 * constraints, and profile synchronization.
 * Side effects: Mutates Member and MemberAddress entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final MemberRepository memberRepository;
    private final MemberAddressRepository memberAddressRepository;
    private final UserMapper userMapper;

    @Override
    public MemberProfileResponse getProfile(UUID memberId) {
        log.info("Fetching profile for memberId: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        List<MemberAddress> addresses = memberAddressRepository.findActiveByMemberId(memberId);
        return userMapper.toProfileResponse(member, addresses);
    }

    @Override
    @Transactional
    public MemberProfileResponse updateProfile(UUID memberId, UpdateProfileRequest request) {
        log.info("Updating profile for memberId: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (request.getVersion() != null && !request.getVersion().equals(member.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The profile was modified by another transaction");
        }

        userMapper.updateMemberEntityFromDTO(request, member);
        Member saved = memberRepository.save(member);
        List<MemberAddress> addresses = memberAddressRepository.findActiveByMemberId(memberId);

        return userMapper.toProfileResponse(saved, addresses);
    }

    @Override
    public List<MemberAddressDTO> getAddresses(UUID memberId) {
        log.info("Fetching addresses for memberId: {}", memberId);
        List<MemberAddress> addresses = memberAddressRepository.findActiveByMemberId(memberId);
        return userMapper.toAddressDTOList(addresses);
    }

    @Override
    @Transactional
    public MemberAddressDTO addAddress(UUID memberId, CreateAddressRequest request) {
        log.info("Adding address for memberId: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", memberId));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            memberAddressRepository.findDefaultAddress(memberId).ifPresent(addr -> {
                addr.setDefault(false);
                memberAddressRepository.save(addr);
            });
        }

        MemberAddress address = userMapper.toAddressEntity(request);
        address.setMember(member);
        MemberAddress saved = memberAddressRepository.save(address);

        return userMapper.toAddressDTO(saved);
    }

    @Override
    @Transactional
    public MemberAddressDTO updateAddress(UUID memberId, UUID addressId, UpdateAddressRequest request) {
        log.info("Updating address: {} for memberId: {}", addressId, memberId);
        MemberAddress address = memberAddressRepository.findActiveByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MemberAddress", addressId));

        if (request.getVersion() != null && !request.getVersion().equals(address.getVersion())) {
            throw new BusinessRuleException("OPTIMISTIC_LOCK_CONFLICT", "The address was modified by another transaction");
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            memberAddressRepository.findDefaultAddress(memberId).ifPresent(addr -> {
                addr.setDefault(false);
                memberAddressRepository.save(addr);
            });
        }

        userMapper.updateAddressEntityFromDTO(request, address);
        MemberAddress saved = memberAddressRepository.save(address);

        return userMapper.toAddressDTO(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(UUID memberId, UUID addressId) {
        log.info("Deleting address: {} for memberId: {}", addressId, memberId);
        MemberAddress address = memberAddressRepository.findActiveByIdAndMemberId(addressId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MemberAddress", addressId));

        address.setDeletedAt(OffsetDateTime.now());
        memberAddressRepository.save(address);
        log.info("Address {} soft-deleted", addressId);
    }
}
