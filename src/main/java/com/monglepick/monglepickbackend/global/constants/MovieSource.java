package com.monglepick.monglepickbackend.global.constants;

/**
 * 영화 데이터 출처 열거형.
 *
 * <p>영화 데이터를 수집한 소스를 분류한다.</p>
 *
 * @see com.monglepick.monglepickbackend.domain.movie.entity.Movie
 */
public enum MovieSource {

    /** TMDB API (포스터, 줄거리, 장르 등) */
    TMDB,

    /** Kaggle MovieLens (평점 데이터) */
    KAGGLE,

    /** KOBIS 영화진흥위원회 (흥행 데이터) */
    KOBIS,

    /** KMDb 한국영화데이터베이스 (한국 영화 정보) */
    KMDB;

    /**
     * 문자열로부터 출처를 안전하게 파싱한다.
     * null이거나 알 수 없는 값이면 null을 반환한다.
     *
     * @param value 출처 문자열 (nullable)
     * @return 파싱된 출처, 알 수 없으면 null
     */
    public static MovieSource fromString(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
