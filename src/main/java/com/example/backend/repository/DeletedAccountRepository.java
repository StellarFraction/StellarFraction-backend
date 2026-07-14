package com.example.backend.repository;

import com.example.backend.model.DeletedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeletedAccountRepository extends JpaRepository<DeletedAccount, Long> {
}
