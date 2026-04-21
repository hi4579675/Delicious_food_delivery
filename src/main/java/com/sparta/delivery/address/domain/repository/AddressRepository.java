package com.sparta.delivery.address.domain.repository;

import com.sparta.delivery.address.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}
