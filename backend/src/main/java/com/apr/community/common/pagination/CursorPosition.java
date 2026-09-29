package com.apr.community.common.pagination;

import java.time.Instant;

public record CursorPosition(Instant createdAt, long id) {
}
