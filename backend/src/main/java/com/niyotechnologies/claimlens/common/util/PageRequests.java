package com.niyotechnologies.claimlens.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a {@link Pageable} from raw {@code page}/{@code size} request params, clamped to safe
 * bounds. Clients (or a crafted request) can pass a negative page or a huge size; without clamping,
 * {@code PageRequest.of} throws on a negative page and a size of 100000 lets one request pull the
 * whole table — defeating the point of paging. Page floors at 0; size is forced into [1, 100].
 */
public final class PageRequests {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(0, page);
        int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
