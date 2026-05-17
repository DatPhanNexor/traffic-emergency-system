package com.example.suco.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.example.suco.model.HoaDon;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class DoanhThuRepository {
    
    @PersistenceContext
    private EntityManager entityManager;

    public BigDecimal getTongDoanhThu() {
        String jpql = "SELECT SUM(h.tongThanhToan) FROM HoaDon h WHERE h.trangThai = 'PAID'";
        Query query = entityManager.createQuery(jpql);
        BigDecimal result = (BigDecimal) query.getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public List<HoaDon> getDanhSachHoaDon() {
        String jpql = "SELECT h FROM HoaDon h ORDER BY h.id DESC";
        return entityManager.createQuery(jpql, HoaDon.class).getResultList();
    }
}
