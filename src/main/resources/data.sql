-- 开发环境测试数据（仅当 spring.sql.init.mode=always，即 dev profile 时执行）
-- 全部写成幂等：已有数据就跳过，服务重启不会插重复记录
-- 生产 profile 下 mode=never，本脚本不会执行

-- 1. 测试用户（测试专用手机号 13800000000）
INSERT INTO users (openid, nickname, phone, gender, status, created_at, updated_at)
SELECT 'test-openid-123', '测试用户', '13800000000', 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE openid = 'test-openid-123');

-- 2. 测试饮食记录：给上面的测试用户补齐「今天」三餐（已有同餐次记录则跳过）
--    用 openid 定位用户而不是写死 user_id=1，避免本地库自增 ID 变化后插到别人名下
INSERT INTO diet_records (user_id, record_date, meal_type, food_name, calories, remark, created_at, updated_at)
SELECT u.id, CURRENT_DATE, s.meal_type, s.food_name, s.calories, s.remark, NOW(), NOW()
FROM users u
JOIN (VALUES
        (1, '燕麦粥', 300, '早餐'),
        (2, '牛肉饭', 700, '午餐'),
        (3, '水果沙拉', 200, '晚餐')
     ) AS s(meal_type, food_name, calories, remark) ON TRUE
WHERE u.openid = 'test-openid-123'
  AND NOT EXISTS (
        SELECT 1 FROM diet_records d
        WHERE d.user_id = u.id
          AND d.record_date = CURRENT_DATE
          AND d.meal_type = s.meal_type
  );
