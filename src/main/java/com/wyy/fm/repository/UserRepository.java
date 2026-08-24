package com.wyy.fm.repository;

import com.wyy.fm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOpenid(String openid);

    Optional<User> findByPhone(String phone);

    boolean existsByOpenid(String openid);
}
