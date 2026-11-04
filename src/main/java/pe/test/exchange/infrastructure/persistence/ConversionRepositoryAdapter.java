package pe.test.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;

@Repository
@RequiredArgsConstructor
public class ConversionRepositoryAdapter implements ConversionRepository {

    private final JpaConversionRepository jpaRepository;

    @Override
    public Conversion save(Conversion conversion) {
        return jpaRepository.save(conversion);
    }
}
