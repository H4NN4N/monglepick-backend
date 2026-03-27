package com.monglepick.monglepickbackend.domain.reward.service;

import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.DeductResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.ExchangeResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.PointItemResponse;
import com.monglepick.monglepickbackend.domain.reward.entity.PointItem;
import com.monglepick.monglepickbackend.domain.reward.repository.PointItemRepository;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import com.monglepick.monglepickbackend.global.exception.InsufficientPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PointItemService 단위 테스트.
 *
 * <p>DB 없이 Mockito로 포인트 아이템 서비스 로직을 검증한다.
 * 캐시({@code @Cacheable})는 단위 테스트 환경에서 활성화되지 않으므로
 * 매 테스트마다 리포지토리 목이 직접 호출된다.</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>{@link PointItemService#getActiveItems} — 활성 아이템 목록 조회</li>
 *   <li>{@link PointItemService#getItemsByCategory} — 카테고리별 아이템 조회</li>
 *   <li>{@link PointItemService#exchangeItem} — 아이템 교환 (정상/잔액 부족/아이템 없음/비활성)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointItemService 단위 테스트")
class PointItemServiceTest {

    /* ── Mocks ── */

    /** 포인트 아이템 리포지토리 목 */
    @Mock
    private PointItemRepository itemRepository;

    /**
     * 포인트 서비스 목.
     *
     * <p>아이템 교환 시 {@link PointService#deductPoint}를 호출하므로 Mock으로 제어한다.</p>
     */
    @Mock
    private PointService pointService;

    /** 테스트 대상 서비스 */
    @InjectMocks
    private PointItemService pointItemService;

    /* ── 공통 픽스처 ── */

    /** 기본 테스트 사용자 ID */
    private static final String USER_ID = "item-user-001";

    /**
     * 활성 PointItem 픽스처 생성 헬퍼.
     *
     * @param id       아이템 ID
     * @param name     아이템명
     * @param price    포인트 가격
     * @param category 카테고리
     */
    private PointItem buildItem(Long id, String name, int price, String category) {
        return PointItem.builder()
                .pointItemId(id)
                .itemName(name)
                .itemDescription(name + " 설명")
                .itemPrice(price)
                .itemCategory(category)
                .isActive(true)
                .build();
    }

    /**
     * DeductResponse 픽스처 — deductPoint() 목 반환값.
     *
     * @param balanceAfter 차감 후 잔액
     * @param historyId    이력 ID
     */
    private DeductResponse deductResponse(int balanceAfter, Long historyId) {
        return new DeductResponse(true, balanceAfter, historyId);
    }

    // ══════════════════════════════════════════════════
    // getActiveItems — 활성 아이템 목록 조회
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("getActiveItems — 활성 아이템 목록 조회")
    class GetActiveItemsTest {

        @Test
        @DisplayName("정상 — 활성 아이템 목록을 가격 오름차순으로 반환")
        void getActiveItems_정상() {
            // given: 2개 아이템 (50P, 100P 순서)
            PointItem item1 = buildItem(1L, "50P 아이템", 50, "general");
            PointItem item2 = buildItem(2L, "100P 아이템", 100, "coupon");
            given(itemRepository.findByIsActiveTrueOrderByItemPriceAsc())
                    .willReturn(List.of(item1, item2));

            // when
            List<PointItemResponse> result = pointItemService.getActiveItems();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).itemId()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("50P 아이템");
            assertThat(result.get(0).price()).isEqualTo(50);
            assertThat(result.get(1).price()).isEqualTo(100);

            verify(itemRepository).findByIsActiveTrueOrderByItemPriceAsc();
        }

        @Test
        @DisplayName("활성 아이템 없음 — 빈 리스트 반환 (예외 미발생)")
        void getActiveItems_빈목록() {
            // given
            given(itemRepository.findByIsActiveTrueOrderByItemPriceAsc())
                    .willReturn(List.of());

            // when
            List<PointItemResponse> result = pointItemService.getActiveItems();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("DTO 변환 — PointItem 엔티티의 모든 필드가 PointItemResponse에 올바르게 매핑됨")
        void getActiveItems_DTO변환() {
            // given: category, description 포함 검증
            PointItem item = buildItem(10L, "AI 이용권", 200, "ai");
            given(itemRepository.findByIsActiveTrueOrderByItemPriceAsc())
                    .willReturn(List.of(item));

            // when
            List<PointItemResponse> result = pointItemService.getActiveItems();

            // then: 모든 필드 매핑 검증
            assertThat(result).hasSize(1);
            PointItemResponse response = result.get(0);
            assertThat(response.itemId()).isEqualTo(10L);
            assertThat(response.name()).isEqualTo("AI 이용권");
            assertThat(response.description()).isEqualTo("AI 이용권 설명");
            assertThat(response.price()).isEqualTo(200);
            assertThat(response.category()).isEqualTo("ai");
        }
    }

    // ══════════════════════════════════════════════════
    // getItemsByCategory — 카테고리별 아이템 조회
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("getItemsByCategory — 카테고리별 아이템 조회")
    class GetItemsByCategoryTest {

        @Test
        @DisplayName("정상 — 특정 카테고리 아이템만 반환")
        void getItemsByCategory_정상() {
            // given: "ai" 카테고리 아이템 2개
            PointItem item1 = buildItem(3L, "AI 1회권", 100, "ai");
            PointItem item2 = buildItem(4L, "AI 5회권", 400, "ai");
            given(itemRepository.findByItemCategoryAndIsActiveTrueOrderByItemPriceAsc("ai"))
                    .willReturn(List.of(item1, item2));

            // when
            List<PointItemResponse> result = pointItemService.getItemsByCategory("ai");

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(r -> r.category().equals("ai"));
        }

        @Test
        @DisplayName("해당 카테고리 아이템 없음 — 빈 리스트 반환")
        void getItemsByCategory_빈목록() {
            // given
            given(itemRepository.findByItemCategoryAndIsActiveTrueOrderByItemPriceAsc("avatar"))
                    .willReturn(List.of());

            // when
            List<PointItemResponse> result = pointItemService.getItemsByCategory("avatar");

            // then
            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════
    // exchangeItem — 아이템 교환
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("exchangeItem — 아이템 교환")
    class ExchangeItemTest {

        @Test
        @DisplayName("정상 — 활성 아이템 존재 + 잔액 충분하면 교환 성공")
        void exchangeItem_정상() {
            // given
            Long itemId = 1L;
            PointItem item = buildItem(itemId, "쿠폰 아이템", 300, "coupon");

            given(itemRepository.findByPointItemIdAndIsActiveTrue(itemId))
                    .willReturn(Optional.of(item));

            // PointService.deductPoint() 목: 차감 성공
            given(pointService.deductPoint(
                    USER_ID,
                    300,
                    "item-" + itemId,
                    "아이템 교환: 쿠폰 아이템"
            )).willReturn(deductResponse(200, 500L));

            // when
            ExchangeResponse response = pointItemService.exchangeItem(USER_ID, itemId);

            // then
            assertThat(response.success()).isTrue();
            assertThat(response.balanceAfter()).isEqualTo(200);  // 500 - 300
            assertThat(response.itemName()).isEqualTo("쿠폰 아이템");

            // 포인트 차감 호출 확인
            verify(pointService).deductPoint(USER_ID, 300, "item-1", "아이템 교환: 쿠폰 아이템");
        }

        @Test
        @DisplayName("아이템 미존재 — BusinessException(ITEM_NOT_FOUND) 발생")
        void exchangeItem_아이템없음() {
            // given: 해당 ID의 활성 아이템이 없음
            Long itemId = 999L;
            given(itemRepository.findByPointItemIdAndIsActiveTrue(itemId))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> pointItemService.exchangeItem(USER_ID, itemId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ITEM_NOT_FOUND));

            // 포인트 차감이 호출되지 않아야 함
            verify(pointService, never()).deductPoint(anyString(), anyInt(), anyString(), anyString());
        }

        @Test
        @DisplayName("비활성 아이템 — findByPointItemIdAndIsActiveTrue가 empty 반환 → ITEM_NOT_FOUND")
        void exchangeItem_비활성아이템() {
            // given: is_active=false인 아이템은 findByPointItemIdAndIsActiveTrue가 empty 반환
            Long itemId = 2L;
            given(itemRepository.findByPointItemIdAndIsActiveTrue(itemId))
                    .willReturn(Optional.empty());  // 비활성이므로 empty

            // when / then
            assertThatThrownBy(() -> pointItemService.exchangeItem(USER_ID, itemId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ITEM_NOT_FOUND));
        }

        @Test
        @DisplayName("잔액 부족 — PointService에서 InsufficientPointException 전파")
        void exchangeItem_잔액부족() {
            // given: 아이템 존재, 포인트 차감 시 잔액 부족 예외 발생
            Long itemId = 3L;
            PointItem item = buildItem(itemId, "프리미엄 아이템", 1000, "general");
            given(itemRepository.findByPointItemIdAndIsActiveTrue(itemId))
                    .willReturn(Optional.of(item));

            // PointService에서 잔액 부족 예외 발생
            given(pointService.deductPoint(
                    USER_ID,
                    1000,
                    "item-" + itemId,
                    "아이템 교환: 프리미엄 아이템"
            )).willThrow(new InsufficientPointException(200, 1000));  // 보유 200, 필요 1000

            // when / then: InsufficientPointException이 그대로 전파되어야 함
            assertThatThrownBy(() -> pointItemService.exchangeItem(USER_ID, itemId))
                    .isInstanceOf(InsufficientPointException.class)
                    .satisfies(ex -> {
                        InsufficientPointException ipex = (InsufficientPointException) ex;
                        assertThat(ipex.getBalance()).isEqualTo(200);
                        assertThat(ipex.getRequired()).isEqualTo(1000);
                        assertThat(ipex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINT);
                    });
        }

        @Test
        @DisplayName("포인트 레코드 없음 — PointService에서 BusinessException(POINT_NOT_FOUND) 전파")
        void exchangeItem_포인트레코드없음() {
            // given: 아이템은 존재하지만 포인트 레코드 없는 사용자
            Long itemId = 4L;
            PointItem item = buildItem(itemId, "일반 아이템", 100, "general");
            given(itemRepository.findByPointItemIdAndIsActiveTrue(itemId))
                    .willReturn(Optional.of(item));

            // PointService에서 포인트 레코드 없음 예외 발생
            given(pointService.deductPoint(
                    USER_ID,
                    100,
                    "item-" + itemId,
                    "아이템 교환: 일반 아이템"
            )).willThrow(new BusinessException(ErrorCode.POINT_NOT_FOUND));

            // when / then
            assertThatThrownBy(() -> pointItemService.exchangeItem(USER_ID, itemId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.POINT_NOT_FOUND));
        }

        @Test
        @DisplayName("아이템 가격 0P — 무료 아이템도 deductPoint 호출 (0원 차감)")
        void exchangeItem_가격0() {
            // given: 가격이 0인 무료 아이템
            Long itemId = 5L;
            PointItem item = buildItem(itemId, "무료 아이템", 0, "general");
            given(itemRepository.findByPointItemIdAndIsActiveTrue(itemId))
                    .willReturn(Optional.of(item));

            // 0 차감 시에도 deductPoint가 호출되어야 함
            given(pointService.deductPoint(
                    USER_ID,
                    0,
                    "item-" + itemId,
                    "아이템 교환: 무료 아이템"
            )).willReturn(deductResponse(500, 600L));

            // when
            ExchangeResponse response = pointItemService.exchangeItem(USER_ID, itemId);

            // then: 0 차감이므로 잔액 변동 없음
            assertThat(response.success()).isTrue();
            assertThat(response.balanceAfter()).isEqualTo(500);
            assertThat(response.itemName()).isEqualTo("무료 아이템");
        }
    }
}
