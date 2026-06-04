package com.example.suco.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.suco.model.LoaiSuCo;

public interface LoaiSuCoRepository extends JpaRepository<LoaiSuCo, Long> {
    Optional<LoaiSuCo> findByTen(String ten);
}