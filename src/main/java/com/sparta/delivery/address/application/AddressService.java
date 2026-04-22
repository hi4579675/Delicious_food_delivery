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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        return AddressResponse.from(addressRepository.save(address));
    }

    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse getAddress(Long userId, UUID addressId) {
        return AddressResponse.from(getOwnedAddress(userId, addressId));
    }

    @Transactional
    public AddressResponse update(Long userId, UUID addressId, AddressUpdateRequest request) {
        Address address = getOwnedAddress(userId, addressId);
        updateAddress(address, request);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, UUID addressId) {
        Address target = getOwnedAddress(userId, addressId);
        changeDefaultAddress(userId, target.getAddressId());
        target.markAsDefault();
        return AddressResponse.from(target);
    }

    @Transactional
    public void delete(Long actorId, String actorRole, UUID addressId) {
        Address address = getDeletableAddress(actorId, actorRole, addressId);
        Long ownerId = address.getUserId();
        boolean wasDefault = address.isDefault();

        address.softDelete(actorId);

        setLatestAddressAsDefault(ownerId, wasDefault);
    }

    private Address getDeletableAddress(Long actorId, String actorRole, UUID addressId) {
        if (isMaster(actorRole)) {
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
                .ifPresent(Address::markAsDefault);
    }

    private Address getOwnedAddress(Long userId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(AddressNotFoundException::new);
        if (!address.getUserId().equals(userId)) {
            throw new AddressForbiddenException();
        }
        return address;
    }

    private static boolean isMaster(String role) {
        return UserRole.valueOf(role) == UserRole.MASTER;
    }
}
