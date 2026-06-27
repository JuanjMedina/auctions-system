package domain.shared;

import java.util.List;
import lombok.Builder;

@Builder
public record PageResult<T>(
    List<T> content, long totalElements, int totalPages, int currentPage, int pageSize) {

  public boolean hasNext() {
    return currentPage < totalPages - 1;
  }

  public boolean hasPrevious() {
    return currentPage > 0;
  }
}
