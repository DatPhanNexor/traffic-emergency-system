package com.example.suco.repository;

import com.example.suco.model.Goi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoiRepository extends JpaRepository<Goi, Long> {
    // JpaRepository đã có sẵn các hàm save, findAll, deleteById...
}