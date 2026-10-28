package com.sim.tjm.repo;

import com.sim.tjm.model.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {
    // Allows you to fetch the entire chronological journey of a single execution
    List<TransactionAudit> findByTransactionIdOrderByTimestampAsc(String transactionId);
}