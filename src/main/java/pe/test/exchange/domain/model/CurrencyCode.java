package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.InvalidCurrencyException;

import java.util.Locale;
import java.util.regex.Pattern;

public record CurrencyCode(String value) {

    private static final Pattern ISO_FORMAT = Pattern.compile("^[A-Z]{3}$");

    public CurrencyCode {
        if (value == null || !ISO_FORMAT.matcher(value).matches()) {
            throw new InvalidCurrencyException(
                    "El código de moneda debe tener 3 letras (ejemplo: USD)");
        }
    }

    public static CurrencyCode of(String raw) {
        if (raw == null) {
            throw new InvalidCurrencyException("El código de moneda es obligatorio");
        }
        return new CurrencyCode(raw.trim().toUpperCase(Locale.ROOT));
    }
}
