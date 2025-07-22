package com.paladin.cv;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CVRepository extends JpaRepository<CV, UUID> {
}
