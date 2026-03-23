package com.monglepick.monglepickbackend.domain.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 영화 엔티티
 *
 * <p>MySQL movies 테이블과 매핑됩니다.
 * 영화의 기본 메타데이터를 저장하며, 157,194건의 영화 데이터가 적재되어 있습니다.</p>
 *
 * <p>데이터 출처:</p>
 * <ul>
 *   <li>TMDB API: 3,617건 (포스터, 줄거리, 장르 등)</li>
 *   <li>Kaggle MovieLens: 42,591건 (평점 데이터)</li>
 *   <li>KMDb 한국영화DB: 36,233건 (한국 영화 정보)</li>
 *   <li>KOBIS 영화진흥위원회: 77,223건 (흥행 데이터)</li>
 * </ul>
 *
 * <p>상세 영화 정보는 Qdrant(벡터)/ES(전문검색)/Neo4j(그래프)에도
 * 저장되어 있으며, 이 테이블은 RDB 기반의 기본 조회에 사용됩니다.</p>
 */
@Entity
@Table(name = "movies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Movie {

    /** 영화 고유 식별자 (VARCHAR(50), DDL에서 PK로 관리) */
    @Id
    @Column(name = "movie_id", length = 50)
    private String movieId;

    /** TMDB 영화 ID (외부 API 연동용) */
    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    /** 영화 한국어 제목 */
    @Column(nullable = false, length = 500)
    private String title;

    /** 영화 영어 원제 */
    @Column(name = "title_en", length = 500)
    private String titleEn;

    /** 영화 줄거리/개요 (TEXT 타입) */
    @Column(columnDefinition = "TEXT")
    private String overview;

    /** 장르 목록 (JSON 배열 형태로 저장, 예: ["액션", "SF"]) */
    @Column(columnDefinition = "JSON")
    private String genres;

    /** 개봉 연도 */
    @Column(name = "release_year")
    private Integer releaseYear;

    /** 평균 평점 (0.0 ~ 10.0) */
    @Column
    private Double rating;

    /** TMDB 포스터 이미지 경로 (예: /abcdef.jpg) */
    @Column(name = "poster_path", length = 500)
    private String posterPath;

    /** 출연진 정보 (JSON 배열) */
    @Column(name = "cast_members", columnDefinition = "JSON")
    private String castMembers;

    /** 감독 정보 */
    @Column(length = 500)
    private String director;

    /** 영화 키워드 (JSON 배열) */
    @Column(columnDefinition = "JSON")
    private String keywords;

    /** OTT 플랫폼 정보 (JSON 배열) */
    @Column(name = "ott_platforms", columnDefinition = "JSON")
    private String ottPlatforms;

    /** 무드 태그 (JSON 배열) */
    @Column(name = "mood_tags", columnDefinition = "JSON")
    private String moodTags;

    /** 데이터 출처 (예: tmdb, kaggle, kobis, kmdb) */
    @Column(length = 50)
    private String source;

    @Builder
    public Movie(String movieId, Long tmdbId, String title, String titleEn, String overview,
                 String genres, Integer releaseYear, Double rating, String posterPath,
                 String castMembers, String director, String keywords,
                 String ottPlatforms, String moodTags, String source) {
        this.movieId = movieId;
        this.tmdbId = tmdbId;
        this.title = title;
        this.titleEn = titleEn;
        this.overview = overview;
        this.genres = genres;
        this.releaseYear = releaseYear;
        this.rating = rating;
        this.posterPath = posterPath;
        this.castMembers = castMembers;
        this.director = director;
        this.keywords = keywords;
        this.ottPlatforms = ottPlatforms;
        this.moodTags = moodTags;
        this.source = source;
    }
}
