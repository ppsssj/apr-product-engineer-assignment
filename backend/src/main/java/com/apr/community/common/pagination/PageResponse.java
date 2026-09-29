package com.apr.community.common.pagination;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, String nextCursor) {
}
