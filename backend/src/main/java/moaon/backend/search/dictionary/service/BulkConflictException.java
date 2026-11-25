package moaon.backend.search.dictionary.service;

import java.util.List;
import moaon.backend.search.dictionary.dto.BulkConflict;

public class BulkConflictException extends RuntimeException {

    private final List<BulkConflict> conflicts;

    public BulkConflictException(List<BulkConflict> conflicts) {
        super("일괄 추가할 항목 중 " + conflicts.size() + "개가 겹칩니다.");
        this.conflicts = conflicts;
    }

    public List<BulkConflict> getConflicts() {
        return conflicts;
    }
}
