package com.kateboo.cloud.community.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;          // 데이터 목록
    private int pageNumber;            // 현재 페이지 (0부터 시작)
    private int pageSize;              // 페이지 크기
    private long totalElements;        // 전체 데이터 수
    private int totalPages;            // 전체 페이지 수
    private boolean first;             // 첫 페이지 여부
    private boolean last;              // 마지막 페이지 여부

    /**
     * Page 객체를 PageResponse로 변환
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    /**
     * Page 객체를 변환 함수를 사용하여 PageResponse로 변환
     */
    public static <T, R> PageResponse<R> of(Page<T> page, Function<T, R> converter) {
        return PageResponse.<R>builder()
                .content(page.getContent().stream()
                        .map(converter)
                        .toList())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}