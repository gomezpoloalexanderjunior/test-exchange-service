package pe.test.exchange.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pe.test.exchange.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "conversions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversion {

    private static final int RESULT_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String sourceCurrency;

    @Column(nullable = false, length = 3)
    private String targetCurrency;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal convertedAmount;

    @Column(nullable = false)
    private Instant operationDate;

    private Conversion(BigDecimal amount, CurrencyCode source, CurrencyCode target,
                       ExchangeRate rate, Instant operationDate) {
        this.amount = amount;
        this.sourceCurrency = source.value();
        this.targetCurrency = target.value();
        this.exchangeRate = rate.value();
        this.convertedAmount = amount.multiply(rate.value()).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
        this.operationDate = operationDate;
    }

    public static Conversion create(BigDecimal amount, CurrencyCode source, CurrencyCode target,
                                    ExchangeRate rate, Instant now) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("El monto debe ser mayor a cero");
        }
        return new Conversion(amount, source, target, rate, now);
    }
}
