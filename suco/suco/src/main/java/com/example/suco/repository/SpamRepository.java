package com.example.suco.repository;

import com.example.suco.model.Spam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpamRepository extends JpaRepository<Spam, Long> {
    // Bạn có thể thêm các hàm tìm kiếm theo userId nếu cần quản lý người hay spam
}