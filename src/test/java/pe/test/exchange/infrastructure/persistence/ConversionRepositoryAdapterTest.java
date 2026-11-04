package pe.test.exchange.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.model.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ConversionRepositoryAdapter.class)
class ConversionRepositoryAdapterTest {

    @Autowired
    private ConversionRepositoryAdapter adapter;

    @Autowired
    private JpaConversionRepository jpaRepository;

    @Test
    void persistsEveryFieldOfTheConversion() {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        Conversion conversion = Conversion.create(new BigDecimal("100"), CurrencyCode.of("USD"),
                CurrencyCode.of("PEN"), new ExchangeRate(new BigDecimal("3.7041")), now);

        Conversion saved = adapter.save(conversion);

        Conversion stored = jpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(stored.getAmount()).isEqualByComparingTo("100");
        assertThat(stored.getSourceCurrency()).isEqualTo("USD");
        assertThat(stored.getTargetCurrency()).isEqualTo("PEN");
        assertThat(stored.getExchangeRate()).isEqualByComparingTo("3.7041");
        assertThat(stored.getConvertedAmount()).isEqualByComparingTo("370.41");
        assertThat(stored.getOperationDate()).isEqualTo(now);
    }
}
