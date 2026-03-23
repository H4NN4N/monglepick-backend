package com.monglepick.monglepickbackend.domain.movie.dto;

import com.monglepick.monglepickbackend.domain.movie.entity.Movie;

/**
 * 영화 정보 응답 DTO
 *
 * <p>영화 조회 및 검색 결과에 반환되는 데이터입니다.</p>
 *
 * @param movieId 영화 ID (VARCHAR(50))
 * @param tmdbId TMDB 영화 ID (외부 연동용)
 * @param title 한국어 제목
 * @param overview 영화 줄거리
 * @param genres 장르 목록 (JSON 문자열)
 * @param releaseYear 개봉 연도
 * @param rating 평균 평점
 * @param posterPath 포스터 이미지 경로
 */
public record MovieResponse(
        String movieId,
        Long tmdbId,
        String title,
        String overview,
        String genres,
        Integer releaseYear,
        Double rating,
        String posterPath
) {
    /**
     * Movie 엔티티로부터 MovieResponse를 생성하는 팩토리 메서드
     *
     * @param movie Movie 엔티티
     * @return MovieResponse 인스턴스
     */
    public static MovieResponse from(Movie movie) {
        return new MovieResponse(
                movie.getMovieId(),
                movie.getTmdbId(),
                movie.getTitle(),
                movie.getOverview(),
                movie.getGenres(),
                movie.getReleaseYear(),
                movie.getRating(),
                movie.getPosterPath()
        );
    }
}
