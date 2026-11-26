package br.com.formentini.eventManager.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableFactory {

    public static final int MAX_PAGE_SIZE = 100;

    private PageableFactory() {
    }

    public static Pageable create(
            int page,
            int size,
            String sortField,
            String direction,
            String... allowedSortFields
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("A página deve ser maior ou igual a zero.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "O tamanho da página deve estar entre 1 e " + MAX_PAGE_SIZE + "."
            );
        }

        Set<String> allowedFields = Set.of(allowedSortFields);
        if (!allowedFields.contains(sortField)) {
            throw new IllegalArgumentException("Campo de ordenação inválido: " + sortField);
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "A direção da ordenação deve ser 'asc' ou 'desc'.",
                    exception
            );
        }

        Sort sort = Sort.by(sortDirection, sortField);
        if (!"id".equals(sortField)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, size, sort);
    }
}
