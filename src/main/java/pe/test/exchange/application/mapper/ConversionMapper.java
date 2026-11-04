package pe.test.exchange.application.mapper;

import org.springframework.stereotype.Component;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;

@Component
public class ConversionMapper {

    public ConversionResponse toResponse(Conversion conversion) {
        return new ConversionResponse(
                conversion.getAmount(),
                conversion.getConvertedAmount(),
                conversion.getSourceCurrency(),
                conversion.getTargetCurrency(),
                conversion.getExchangeRate());
    }
}
