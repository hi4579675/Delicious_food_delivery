package com.sparta.delivery.address.application;

import com.sparta.delivery.address.domain.entity.Address;
import com.sparta.delivery.address.domain.exception.AddressForbiddenException;
import com.sparta.delivery.address.domain.exception.AddressNotFoundException;
import com.sparta.delivery.address.domain.repository.AddressRepository;
import com.sparta.delivery.address.presentation.dto.AddressCreateRequest;
import com.sparta.delivery.address.presentation.dto.AddressResponse;
import com.sparta.delivery.address.presentation.dto.AddressUpdateRequest;
import java.util.List;
import java.util.UUID;

import com.sparta.delivery.user.domain.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional
    public AddressResponse create(Long userId, AddressCreateRequest request) {
        boolean shouldBeDefault = shouldSetAsDefault(userId, request.isDefault());
        if (shouldBeDefault) {
            clearDefaultAddress(userId);
        }

        Address address = createAddress(userId, request, shouldBeDefault);
        Address savedAddress = addressRepository.save(address);
        log.info("배송지 등록 완료 - userId={}, addressId={}, 기본배송지={}",
                userId, savedAddress.getAddressId(), savedAddress.isDefault());
        return AddressResponse.from(savedAddress);
    }

    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public Address findOwnedAddress(Long userId, UUID addressId) {
        return getOwnedAddress(userId, addressId);
    }

    public AddressResponse getAddress(Long userId, UUID addressId) {
        return AddressResponse.from(getOwnedAddress(userId, addressId));
    }

    @Transactional
    public AddressResponse update(Long userId, UUID addressId, AddressUpdateRequest request) {
        Address address = getOwnedAddress(userId, addressId);
        updateAddress(address, request);
        log.info("배송지 수정 완료 - userId={}, addressId={}", userId, addressId);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, UUID addressId) {
        Address target = getOwnedAddress(userId, addressId);
        changeDefaultAddress(userId, target.getAddressId());
        target.markAsDefault();
        log.info("기본 배송지 변경 완료 - userId={}, addressId={}", userId, addressId);
        return AddressResponse.from(target);
    }

    @Transactional
    public void delete(Long actorId, UserRole actorRole, UUID addressId) {
        Address address = getDeletableAddress(actorId, actorRole, addressId);
        Long ownerId = address.getUserId();
        boolean wasDefault = address.isDefault();

        address.softDelete(actorId);
        log.info("배송지 삭제 완료 - actorId={}, addressId={}, 기본배송지여부={}",
                actorId, addressId, wasDefault);

        setLatestAddressAsDefault(ownerId, wasDefault);
    }

    private Address getDeletableAddress(Long actorId, UserRole actorRole, UUID addressId) {
        if (actorRole == UserRole.MASTER) {
            return addressRepository.findById(addressId)
                    .orElseThrow(AddressNotFoundException::new);
        }
        return getOwnedAddress(actorId, addressId);
    }

    private boolean shouldSetAsDefault(Long userId, boolean requestedDefault) {
        // 첫 배송지는 기본 배송지 1개를 보장하기 위해 자동으로 기본 처리한다.
        return requestedDefault || !addressRepository.existsByUserId(userId);
    }

    private Address createAddress(Long userId, AddressCreateRequest request, boolean isDefault) {
        return Address.create(
                userId,
                request.alias(),
                request.address(),
                request.detail(),
                request.zipCode(),
                isDefault
        );
    }

    private void updateAddress(Address address, AddressUpdateRequest request) {
        address.update(request.alias(), request.address(), request.detail(), request.zipCode());
    }

    private void clearDefaultAddress(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(Address::unmarkAsDefault);
    }

    private void changeDefaultAddress(Long userId, UUID targetAddressId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(current -> {
            if (!current.getAddressId().equals(targetAddressId)) {
                current.unmarkAsDefault();
            }
        });
    }

    private void setLatestAddressAsDefault(Long ownerId, boolean deletedDefault) {
        if (!deletedDefault) {
            return;
        }

        addressRepository.findAllByUserIdOrderByCreatedAtDesc(ownerId).stream()
                .findFirst()
                .ifPresent(address -> {
                    address.markAsDefault();
                    log.info("기본 배송지 재지정 완료 - userId={}, addressId={}",
                            ownerId, address.getAddressId());
                });
    }

    private Address getOwnedAddress(Long userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(AddressNotFoundException::new);
        if (!address.getUserId().equals(userId)) {
            throw new AddressForbiddenException();
        }
        return address;
    }
}
