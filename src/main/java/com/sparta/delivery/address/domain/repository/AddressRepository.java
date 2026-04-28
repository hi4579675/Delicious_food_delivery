package com.sparta.delivery.address.domain.repository;

import com.sparta.delivery.address.domain.entity.Address;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);

    boolean existsByUserId(Long userId);
}
