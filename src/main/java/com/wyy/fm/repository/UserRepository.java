package com.wyy.fm.repository;

import com.wyy.fm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问层
 * 
 * 作用：操作 users 表（增删改查）
 * 
 * 继承 JpaRepository 后自动获得的方法：
 * - save(user)：插入或更新
 * - findById(id)：根据 ID 查询
 * - findAll()：查询所有
 * - delete(user)：删除
 * - count()：统计总数
 * ... 等等
 * 
 * 自定义查询方法（Spring Data JPA 根据方法名自动生成 SQL）：
 * - findByOpenid(openid) → SELECT * FROM users WHERE openid = ?
 * - findByPhone(phone) → SELECT * FROM users WHERE phone = ?
 * - existsByOpenid(openid) → SELECT COUNT(*) > 0 FROM users WHERE openid = ?
 * 
 * 方法命名规则：
 * - findBy + 字段名：根据字段查询
 * - findBy + 字段名 + And/Or：多条件查询
 * - existsBy + 字段名：判断是否存在
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据 openid 查询用户
     * 
     * @param openid 微信 openid
     * @return Optional<User>：可能找到（Optional.of）或没找到（Optional.empty）
     * 
     * 使用示例：
     * User user = userRepository.findByOpenid("xxx").orElse(null);
     * // 或
     * User user = userRepository.findByOpenid("xxx").orElseThrow(() -> new RuntimeException("用户不存在"));
     */
    Optional<User> findByOpenid(String openid);

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 判断 openid 是否已存在
     * 
     * @return true：已存在（老用户），false：不存在（新用户）
     */
    boolean existsByOpenid(String openid);
}
