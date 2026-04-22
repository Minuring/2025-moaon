package moaon.backend.search.dictionary.service;

import moaon.backend.search.dictionary.dto.SynonymEntry;

public class DuplicateSynonymEntryException extends RuntimeException {

    private final SynonymEntry conflictEntry;
    private final String duplicateTerm;

    public DuplicateSynonymEntryException(SynonymEntry conflictEntry, String duplicateTerm) {
        super("이미 등록된 term과 겹칩니다: " + duplicateTerm);
        this.conflictEntry = conflictEntry;
        this.duplicateTerm = duplicateTerm;
    }

    public SynonymEntry getConflictEntry() {
        return conflictEntry;
    }

    public String getDuplicateTerm() {
        return duplicateTerm;
    }
}
