package com.example.suco.repository;

import com.example.suco.model.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CameraRepository extends JpaRepository<Camera, Long> {
    List<Camera> findByGeohashIn(List<String> geohashes);
    List<Camera> findByKinhDoIsNull();
}