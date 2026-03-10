package com.walter.lifelog.shared.paging;

import java.util.List;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalCount,
    int totalPages
) {}