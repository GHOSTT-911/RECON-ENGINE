package com.reconengine.repository;

import com.reconengine.model.Transaction;
import com.reconengine.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findAllByOrderByIdAsc();

    List<Transaction> findByStatusOrderByIdAsc(TransactionStatus status);

    long countByStatus(TransactionStatus status);
}
