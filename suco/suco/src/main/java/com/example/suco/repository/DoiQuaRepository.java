package com.example.suco.repository;

import com.example.suco.model.DoiQua;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoiQuaRepository extends JpaRepository<DoiQua, Long> {
    List<DoiQua> findByUserId(String userId);
    Optional<DoiQua> findByUserIdAndQuaId(String userId, Long quaId);
    @Query("""
SELECT d, q
FROM DoiQua d
JOIN Qua q ON d.quaId = q.id
WHERE d.userId = :uid
""")
List<Object[]> getMyGiftsWithQua(@Param("uid") String uid);
}