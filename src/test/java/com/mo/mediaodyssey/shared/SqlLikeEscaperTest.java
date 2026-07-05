package com.mo.mediaodyssey.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.mo.mediaodyssey.shared.util.SqlLikeEscaper;

class SqlLikeEscaperTest {

    @Test
    void escape_escapesLikeMetacharacters() {
        assertThat(SqlLikeEscaper.escape("50%_\\off")).isEqualTo("50\\%\\_\\\\off");
    }

    @Test
    void escape_nullReturnsNull() {
        assertThat(SqlLikeEscaper.escape(null)).isNull();
    }
}
