package com.walter.lifelog.shared.paging;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "페이징 응답")
public record PageResponse<T>(
    @Schema(description = "조회 결과 목록")
    List<T> content,
    @Schema(description = "현재 페이지 번호", example = "1")
    int page,
    @Schema(description = "페이지 크기", example = "10")
    int size,
    @Schema(description = "전체 데이터 수", example = "100")
    long totalCount,
    @Schema(description = "전체 페이지 수", example = "10")
    int totalPages
) {}