package com.example.suco.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.suco.model.Goi;
import com.example.suco.model.User;
import com.example.suco.repository.GoiRepository;
import com.example.suco.repository.MuaGoiRepository;
import com.example.suco.repository.UserRepository;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MuaGoiRepository muaGoiRepository;

    @Autowired
    private GoiRepository goiRepository;
    
    public boolean existsById(String uid) {
    return userRepository.existsById(uid);
}

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAllByProviderNot("SYSTEM");
        populatePackageInfo(users);
        return users;
    }

    public List<User> searchUsers(String keyword) {
        List<User> users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndProviderNot(keyword, keyword, "SYSTEM");
        populatePackageInfo(users);
        return users;
    }

    private void populatePackageInfo(List<User> users) {
        for (User user : users) {
            // Tìm gói đang ACTIVE của user
            var activePackage = muaGoiRepository.findFirstByUserIdAndTrangThai(user.getUid(), "ACTIVE");
            if (activePackage.isPresent()) {
                String tenGoi = goiRepository.findById(activePackage.get().getGoiId())
                        .map(Goi::getTen).orElse("Gói không xác định");
                user.setTenGoi(tenGoi);
            } else {
                user.setTenGoi(null);
            }
        }
    }

    public void deleteUser(String uid) {
        userRepository.deleteById(uid);
    }

    public void resetSpamCount(String uid) {
        userRepository.findById(uid).ifPresent(user -> {
            user.setSpamCount(0);
            userRepository.save(user);
        });
    }
    // Thêm hàm này vào UserService
public User getUserInfo(String uid) {
    return userRepository.findById(uid).map(user -> {

        // 👉 LẤY DATA THẬT TỪ DB (user đã có sẵn)
        // KHÔNG GÁN = 0 nữa
        System.out.println("=== USER FROM DB ===");
System.out.println("UID: " + user.getUid());
System.out.println("POINTS: " + user.getTotalPoints());
System.out.println("SPAM: " + user.getSpamCount());

        var activePackage = muaGoiRepository.findFirstByUserIdAndTrangThai(user.getUid(), "ACTIVE");

        if (activePackage.isPresent()) {
            String tenGoi = goiRepository.findById(activePackage.get().getGoiId())
                    .map(Goi::getTen).orElse("Premium");
            user.setTenGoi(tenGoi);
        } else {
            user.setTenGoi(null);
        }

        return user;

    }).orElse(null);
}
}
