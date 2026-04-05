package com.hotel.repository;

import com.hotel.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    @Query("SELECT o FROM Otp o WHERE o.email = :email AND o.purpose = :purpose AND o.verified = false ORDER BY o.createdAt DESC LIMIT 1")
    Optional<Otp> findLatestByEmailAndPurpose(@Param("email") String email, @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expirationTime < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Otp o SET o.verified = true WHERE o.email = :email AND o.purpose = :purpose")
    void markAsVerified(@Param("email") String email, @Param("purpose") String purpose);
}
