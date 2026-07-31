package com.niyotechnologies.claimlens.common.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable, serialization-friendly page envelope returned by list endpoints. We map Spring's
 * {@link Page} into this record rather than exposing {@code Page}/{@code PageImpl} directly — its
 * JSON shape is not part of Spring Data's API contract and changes between versions. The fields a
 * client actually needs to render paging controls are all here: the rows, where we are, and how
 * many pages/rows exist in total (so the UI can clamp an out-of-range page after a delete).
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /** Map a fetched {@code Page<E>} of entities into a page of DTOs. */
    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    /**
     * Take the paging metadata from {@code page} but the already-mapped {@code content}. Used when the
     * content is produced by a batch mapper (one that loads a whole page's references at once to avoid
     * an N+1), so the per-element {@link #from} signature doesn't fit.
     */
    public static <T> PagedResponse<T> of(Page<?> page, List<T> content) {
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
