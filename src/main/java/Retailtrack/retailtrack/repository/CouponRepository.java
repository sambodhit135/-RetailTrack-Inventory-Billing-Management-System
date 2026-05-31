package Retailtrack.retailtrack.repository;

import Retailtrack.retailtrack.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND (c.expiryDate IS NULL OR c.expiryDate >= :currentDate)")
    List<Coupon> findActiveCoupons(@Param("currentDate") LocalDate currentDate);
}
