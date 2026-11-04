package pe.test.exchange.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.mapper.ConversionMapper;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.CurrencyConversionService;

@Service
@RequiredArgsConstructor
public class ConvertCurrencyService {

    private final CurrencyConversionService conversionService;
    private final ConversionRepository repository;
    private final ConversionMapper mapper;

    public ConversionResponse convert(ConversionRequest request) {
        CurrencyCode source = CurrencyCode.of(request.monedaOrigen());
        CurrencyCode target = CurrencyCode.of(request.monedaDestino());

        Conversion conversion = conversionService.convert(request.monto(), source, target);

        return mapper.toResponse(repository.save(conversion));
    }
}
