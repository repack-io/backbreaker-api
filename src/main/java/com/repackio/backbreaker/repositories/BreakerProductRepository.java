package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.BreakerProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BreakerProductRepository extends JpaRepository<BreakerProduct, Integer> {
}
