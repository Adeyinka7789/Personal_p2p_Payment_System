// REVIEW: full code review requested

package com.example.ppps.repository;

import com.example.ppps.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;


@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
}