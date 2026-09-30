package com.apr.community.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostSearchPatternTest {

    @Test
    void wrapsQueryForSubstringMatch() {
        assertThat(PostRepository.containsPattern("선크림")).isEqualTo("%선크림%");
        assertThat(PostRepository.containsPattern(null)).isNull();
    }

    @Test
    void escapesLikeMetacharactersSoTheyMatchLiterally() {
        assertThat(PostRepository.containsPattern("100%")).isEqualTo("%100\\%%");
        assertThat(PostRepository.containsPattern("a_b")).isEqualTo("%a\\_b%");
        assertThat(PostRepository.containsPattern("c:\\d")).isEqualTo("%c:\\\\d%");
    }
}
