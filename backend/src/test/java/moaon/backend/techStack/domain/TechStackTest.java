package moaon.backend.techStack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TechStackTest {

    @DisplayName("normalize: 공백으로 구분된 이름을 camelCase로 변환한다")
    @Test
    void normalize_withSpaces() {
        assertThat(TechStack.normalize("tanstack query")).isEqualTo("tanstackQuery");
    }

    @DisplayName("normalize: 언더스코어로 구분된 이름을 camelCase로 변환한다")
    @Test
    void normalize_withUnderscore() {
        assertThat(TechStack.normalize("tanstack_query")).isEqualTo("tanstackQuery");
    }

    @DisplayName("normalize: 하이픈과 대소문자가 섞인 이름을 camelCase로 변환한다")
    @Test
    void normalize_withHyphenAndMixedCase() {
        assertThat(TechStack.normalize("TanStack-Query")).isEqualTo("tanstackQuery");
    }

    @DisplayName("normalize: 전부 대문자인 이름을 camelCase로 변환한다")
    @Test
    void normalize_allUppercase() {
        assertThat(TechStack.normalize("TANSTACK QUERY")).isEqualTo("tanstackQuery");
    }

    @DisplayName("normalize: 단일 단어는 소문자로만 변환한다")
    @Test
    void normalize_singleWord() {
        assertThat(TechStack.normalize("Java")).isEqualTo("java");
    }
}
