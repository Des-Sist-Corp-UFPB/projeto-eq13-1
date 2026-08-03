package br.ufpb.dsc.jobhub.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseHealthServiceTest {

    @Test
    void checksDatabaseWithSelectOne() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DatabaseHealthService service = new DatabaseHealthService(jdbcTemplate);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1, 0);

        assertThat(service.isDatabaseAvailable()).isTrue();
        assertThat(service.isDatabaseAvailable()).isFalse();
    }
}
