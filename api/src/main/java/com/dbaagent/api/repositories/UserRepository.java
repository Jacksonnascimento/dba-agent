package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // O "JOIN FETCH" força o Hibernate a trazer o Usuário e o Tenant na mesma query,
    // resolvendo o erro de "no session" lá no Controller!
    @Query("SELECT u FROM User u JOIN FETCH u.tenant WHERE u.email = :email")
    Optional<User> findByEmail(String email);
}