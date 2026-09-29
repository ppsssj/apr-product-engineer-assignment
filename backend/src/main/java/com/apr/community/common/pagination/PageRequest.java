package com.apr.community.common.pagination;

public record PageRequest(int limit, CursorPosition cursor) {
}
