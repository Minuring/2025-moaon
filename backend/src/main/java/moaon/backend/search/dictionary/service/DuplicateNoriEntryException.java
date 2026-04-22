package moaon.backend.search.dictionary.service;

import moaon.backend.search.dictionary.dto.NoriEntry;

public class DuplicateNoriEntryException extends RuntimeException {

    private final NoriEntry conflictEntry;

    public DuplicateNoriEntryException(NoriEntry conflictEntry) {
        super("이미 등록된 표현과 겹칩니다: " + conflictEntry.getSurface());
        this.conflictEntry = conflictEntry;
    }

    public NoriEntry getConflictEntry() {
        return conflictEntry;
    }
}
