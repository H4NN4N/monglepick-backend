-- ============================================================
-- 몽글픽 시드 데이터 (Spring Boot 기동 시 자동 실행)
-- ============================================================
-- ddl-auto=update로 테이블 생성 후, 초기 시드 데이터를 적재한다.
-- INSERT IGNORE로 중복 실행해도 안전하다.
-- ============================================================

-- ── 포인트 교환 아이템 ──
INSERT IGNORE INTO point_items (item_name, item_description, item_price, item_category)
VALUES
    ('AI 추천 1회',     'AI 영화 추천 1회 이용',        100, 'ai_feature'),
    ('AI 추천 5회 팩',  'AI 영화 추천 5회 이용 (10% 할인)', 450, 'ai_feature'),
    ('프로필 테마',     '프로필 커스텀 테마 적용',       200, 'profile'),
    ('칭호 변경',       '커뮤니티 닉네임 칭호 변경',     150, 'profile'),
    ('도장깨기 힌트',   '퀴즈 힌트 1회 사용',            50, 'roadmap');

-- ── 구독 상품 ──
INSERT IGNORE INTO subscription_plans (plan_code, name, period_type, price, points_per_period, description)
VALUES
    ('monthly_basic',   '월간 기본',     'MONTHLY',  3900,  3000,   '매월 3,000 포인트 지급 (AI 추천 30회)'),
    ('monthly_premium', '월간 프리미엄',  'MONTHLY',  7900,  8000,   '매월 8,000 포인트 지급 (AI 추천 80회)'),
    ('yearly_basic',    '연간 기본',     'YEARLY',   39000, 40000,  '연간 40,000 포인트 지급 (AI 추천 400회)'),
    ('yearly_premium',  '연간 프리미엄',  'YEARLY',   79000, 100000, '연간 100,000 포인트 지급 (AI 추천 1,000회)');
