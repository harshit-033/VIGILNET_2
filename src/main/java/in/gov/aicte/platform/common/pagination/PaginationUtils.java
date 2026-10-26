package in.gov.aicte.platform.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationUtils {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationUtils() {
    }

    public static Pageable createPageable(Integer page, Integer size, Sort defaultSort) {
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_PAGE_SIZE;
        Sort sort = (defaultSort != null) ? defaultSort : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
