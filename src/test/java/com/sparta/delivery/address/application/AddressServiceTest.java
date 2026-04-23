package com.sparta.delivery.address.application;

import com.sparta.delivery.address.domain.entity.Address;
import com.sparta.delivery.address.domain.exception.AddressForbiddenException;
import com.sparta.delivery.address.domain.exception.InvalidAddressException;
import com.sparta.delivery.address.domain.repository.AddressRepository;
import com.sparta.delivery.address.presentation.dto.AddressCreateRequest;
import com.sparta.delivery.address.presentation.dto.AddressResponse;
import com.sparta.delivery.address.presentation.dto.AddressUpdateRequest;
import com.sparta.delivery.user.domain.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private AddressService addressService;

    @Nested
    @DisplayName("배송지 등록")
    class Create {

        @Test
        @DisplayName("첫 배송지를 등록하면 기본 배송지로 저장된다")
        void success() {
            // given
            Long userId = 1L;
            AddressCreateRequest request = new AddressCreateRequest(
                    "집",
                    "서울시 강남구",
                    "101호",
                    "12345",
                    false
            );

            given(addressRepository.existsByUserId(userId)).willReturn(false);
            given(addressRepository.save(any(Address.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            AddressResponse response = addressService.create(userId, request);

            // then
            assertThat(response.isDefault()).isTrue();
            assertThat(response.alias()).isEqualTo("집");
            assertThat(response.address()).isEqualTo("서울시 강남구");
        }

        @Test
        @DisplayName("주소가 비어 있으면 예외가 발생한다")
        void invalidAddress() {
            // given
            Long userId = 1L;
            AddressCreateRequest request = new AddressCreateRequest(
                    "집",
                    "   ",
                    "101호",
                    "12345",
                    false
            );

            given(addressRepository.existsByUserId(userId)).willReturn(false);

            // when // then
            assertThatThrownBy(() -> addressService.create(userId, request))
                    .isInstanceOf(InvalidAddressException.class);
        }
    }

    @Test
    @DisplayName("본인 배송지를 상세 조회할 수 있다")
    void getAddress() {
        // given
        Long userId = 1L;
        UUID addressId = UUID.randomUUID();
        Address address = Address.create(
                userId,
                "집",
                "서울시 강남구",
                "101호",
                "12345",
                true
        );
        setAddressId(address, addressId);

        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

        // when
        AddressResponse response = addressService.getAddress(userId, addressId);

        // then
        assertThat(response.alias()).isEqualTo("집");
        assertThat(response.address()).isEqualTo("서울시 강남구");
        assertThat(response.isDefault()).isTrue();
    }

    @Nested
    @DisplayName("배송지 수정")
    class Update {

        @Test
        @DisplayName("본인 배송지를 수정할 수 있다")
        void success() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            Address address = Address.create(
                    userId,
                    "집",
                    "서울시 강남구",
                    "101호",
                    "12345",
                    true
            );
            setAddressId(address, addressId);
            AddressUpdateRequest request = new AddressUpdateRequest(
                    "회사",
                    "서울시 서초구",
                    "202호",
                    "54321"
            );

            given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

            // when
            AddressResponse response = addressService.update(userId, addressId, request);

            // then
            assertThat(response.alias()).isEqualTo("회사");
            assertThat(response.address()).isEqualTo("서울시 서초구");
            assertThat(response.detail()).isEqualTo("202호");
            assertThat(response.zipCode()).isEqualTo("54321");
        }

        @Test
        @DisplayName("본인 배송지가 아니면 수정할 수 없다")
        void forbidden() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            Address address = Address.create(
                    2L,
                    "집",
                    "서울시 강남구",
                    "101호",
                    "12345",
                    true
            );
            setAddressId(address, addressId);
            AddressUpdateRequest request = new AddressUpdateRequest(
                    "회사",
                    "서울시 서초구",
                    "202호",
                    "54321"
            );

            given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

            // when // then
            assertThatThrownBy(() -> addressService.update(userId, addressId, request))
                    .isInstanceOf(AddressForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("기본 배송지 설정")
    class SetDefaultAddress {

        @Test
        @DisplayName("기존 기본 배송지를 해제하고 새 배송지를 기본으로 설정한다")
        void success() {
            // given
            Long userId = 1L;
            UUID targetAddressId = UUID.randomUUID();
            Address currentDefault = Address.create(
                    userId,
                    "집",
                    "서울시 강남구",
                    "101호",
                    "12345",
                    true
            );
            setAddressId(currentDefault, UUID.randomUUID());
            Address target = Address.create(
                    userId,
                    "회사",
                    "서울시 서초구",
                    "202호",
                    "54321",
                    false
            );
            setAddressId(target, targetAddressId);

            given(addressRepository.findById(targetAddressId)).willReturn(Optional.of(target));
            given(addressRepository.findByUserIdAndIsDefaultTrue(userId)).willReturn(Optional.of(currentDefault));

            // when
            AddressResponse response = addressService.setDefaultAddress(userId, targetAddressId);

            // then
            assertThat(currentDefault.isDefault()).isFalse();
            assertThat(target.isDefault()).isTrue();
            assertThat(response.isDefault()).isTrue();
            assertThat(response.alias()).isEqualTo("회사");
        }

        @Test
        @DisplayName("본인 배송지가 아니면 기본 배송지로 설정할 수 없다")
        void forbidden() {
            // given
            Long userId = 1L;
            UUID targetAddressId = UUID.randomUUID();
            Address target = Address.create(
                    2L,
                    "회사",
                    "서울시 서초구",
                    "202호",
                    "54321",
                    false
            );
            setAddressId(target, targetAddressId);

            given(addressRepository.findById(targetAddressId)).willReturn(Optional.of(target));

            // when // then
            assertThatThrownBy(() -> addressService.setDefaultAddress(userId, targetAddressId))
                    .isInstanceOf(AddressForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("배송지 삭제")
    class Delete {

        @Test
        @DisplayName("기본 배송지를 삭제하면 최근 배송지가 기본 배송지로 변경된다")
        void success() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            Address deletedAddress = Address.create(
                    userId,
                    "집",
                    "서울시 강남구",
                    "101호",
                    "12345",
                    true
            );
            setAddressId(deletedAddress, addressId);
            Address latestAddress = Address.create(
                    userId,
                    "회사",
                    "서울시 서초구",
                    "202호",
                    "54321",
                    false
            );
            setAddressId(latestAddress, UUID.randomUUID());

            given(addressRepository.findById(addressId)).willReturn(Optional.of(deletedAddress));
            given(addressRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
                    .willReturn(List.of(latestAddress));

            // when
            addressService.delete(userId, UserRole.CUSTOMER, addressId);

            // then
            assertThat(deletedAddress.isDeleted()).isTrue();
            assertThat(deletedAddress.getDeletedBy()).isEqualTo(userId);
            assertThat(latestAddress.isDefault()).isTrue();
        }

        @Test
        @DisplayName("본인 배송지가 아니면 삭제할 수 없다")
        void forbidden() {
            // given
            Long userId = 1L;
            UUID addressId = UUID.randomUUID();
            Address address = Address.create(
                    2L,
                    "집",
                    "서울시 강남구",
                    "101호",
                    "12345",
                    true
            );
            setAddressId(address, addressId);

            given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

            // when // then
            assertThatThrownBy(() -> addressService.delete(userId, UserRole.CUSTOMER, addressId))
                    .isInstanceOf(AddressForbiddenException.class);
        }
    }

    private void setAddressId(Address address, UUID addressId) {
        ReflectionTestUtils.setField(address, "addressId", addressId);
    }
}
