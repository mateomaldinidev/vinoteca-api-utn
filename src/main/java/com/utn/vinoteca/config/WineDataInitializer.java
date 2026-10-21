package com.utn.vinoteca.config;

import com.utn.vinoteca.features.distributor.entity.DistributorEntity;
import com.utn.vinoteca.features.distributor.repository.DistributorRepository;
import com.utn.vinoteca.features.grapeVariety.entity.GrapeVarietyEntity;
import com.utn.vinoteca.features.grapeVariety.repository.GrapeVarietyRepository;
import com.utn.vinoteca.features.wine.entity.WineEntity;
import com.utn.vinoteca.features.wine.repository.WineRepository;
import com.utn.vinoteca.features.wineType.entity.WineTypeEntity;
import com.utn.vinoteca.features.wineType.repository.WineTypeRepository;
import com.utn.vinoteca.features.wineVariant.entity.WineVariantEntity;
import com.utn.vinoteca.features.wineVariant.repository.WineVariantRepository;
import com.utn.vinoteca.features.winery.entity.WineryEntity;
import com.utn.vinoteca.features.winery.repository.WineryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class WineDataInitializer {

    private final WineTypeRepository wineTypeRepository;
    private final GrapeVarietyRepository grapeVarietyRepository;
    private final WineryRepository wineryRepository;
    private final DistributorRepository distributorRepository;
    private final WineRepository wineRepository;
    private final WineVariantRepository wineVariantRepository;

    @Bean
    CommandLineRunner initWineData() {
        return args -> {
            WineTypeEntity tinto = wineType("Tinto");
            WineTypeEntity blanco = wineType("Blanco");
            WineTypeEntity rosado = wineType("Rosado");
            WineTypeEntity espumante = wineType("Espumante");
            wineType("Dulce");

            GrapeVarietyEntity malbec = grapeVariety("Malbec");
            GrapeVarietyEntity cabernet = grapeVariety("Cabernet Sauvignon");
            GrapeVarietyEntity merlot = grapeVariety("Merlot");
            grapeVariety("Syrah");
            grapeVariety("Bonarda");
            grapeVariety("Tempranillo");
            GrapeVarietyEntity chardonnay = grapeVariety("Chardonnay");
            GrapeVarietyEntity sauvignonBlanc = grapeVariety("Sauvignon Blanc");
            GrapeVarietyEntity torrontes = grapeVariety("Torrontés");
            GrapeVarietyEntity pinotNoir = grapeVariety("Pinot Noir");

            WineryEntity zuccardi = winery("Zuccardi");
            WineryEntity catena = winery("Catena Zapata");
            WineryEntity trapiche = winery("Trapiche");
            WineryEntity luigiBosca = winery("Luigi Bosca");
            WineryEntity rutini = winery("Rutini");

            DistributorEntity oeste = distributor("Distribuidora del Oeste", "ventas@distribuidoraoeste.com");
            DistributorEntity plata = distributor("Bodegas del Plata", "contacto@bodegasdelplata.com");

            wine("Malbec Reserva", "Malbec de guarda con notas de frutos rojos", 2021, "Valle de Uco",
                    bd("14.0"), "Aromas de ciruela y violetas, final persistente",
                    tinto, zuccardi, oeste, Set.of(malbec),
                    List.of(
                            new VariantSeed("Botella 750 ml", bd("12500.00"), 40, bd("11000.00"), 6),
                            new VariantSeed("Magnum 1500 ml", bd("24000.00"), 10, null, null)
                    ));

            wine("Catena Cabernet Sauvignon", "Cabernet Sauvignon de altura", 2019, "Luján de Cuyo",
                    bd("14.2"), "Taninos firmes, notas de cassis y especias",
                    tinto, catena, oeste, Set.of(cabernet),
                    List.of(new VariantSeed("Botella 750 ml", bd("18500.00"), 24, bd("16500.00"), 6)));

            wine("Trapiche Corte Malbec Cabernet", "Corte clásico de Malbec y Cabernet Sauvignon", 2020, "Mendoza",
                    bd("13.8"), "Frutas rojas maduras con un final especiado",
                    tinto, trapiche, plata, Set.of(malbec, cabernet),
                    List.of(new VariantSeed("Botella 750 ml", bd("9500.00"), 60, bd("8200.00"), 12)));

            wine("Luigi Bosca Chardonnay", "Chardonnay fresco de Valle de Uco", 2022, "Valle de Uco",
                    bd("13.2"), "Notas cítricas y un paso por roble sutil",
                    blanco, luigiBosca, oeste, Set.of(chardonnay),
                    List.of(new VariantSeed("Botella 750 ml", bd("11200.00"), 30, null, null)));

            wine("Rutini Sauvignon Blanc", "Sauvignon Blanc patagónico", 2023, "Patagonia",
                    bd("12.8"), "Aromas herbáceos y fruta tropical",
                    blanco, rutini, plata, Set.of(sauvignonBlanc),
                    List.of(new VariantSeed("Botella 750 ml", bd("10800.00"), 35, null, null)));

            wine("Zuccardi Rosado de Malbec", "Rosado joven de Malbec", 2023, "Mendoza",
                    bd("12.5"), "Fresco, con notas de frutilla y cereza",
                    rosado, zuccardi, oeste, Set.of(malbec),
                    List.of(new VariantSeed("Botella 750 ml", bd("8900.00"), 25, null, null)));

            wine("Rutini Extra Brut", "Espumante de método tradicional", 2021, "Mendoza",
                    bd("12.0"), "Burbuja fina, notas de pan tostado y cítricos",
                    espumante, rutini, oeste, Set.of(chardonnay, pinotNoir),
                    List.of(new VariantSeed("Botella 750 ml", bd("15600.00"), 18, null, null)));

            wine("Trapiche Torrontés", "Torrontés de altura", 2023, "Salta",
                    bd("13.0"), "Aromas florales y fruta blanca",
                    blanco, trapiche, plata, Set.of(torrontes),
                    List.of(new VariantSeed("Botella 750 ml", bd("7800.00"), 20, null, null)));

            wine("Luigi Bosca Merlot", "Merlot de guarda", 2020, "Luján de Cuyo",
                    bd("13.6"), "Ciruela madura y taninos suaves",
                    tinto, luigiBosca, plata, Set.of(merlot),
                    List.of(new VariantSeed("Botella 750 ml", bd("13400.00"), 22, bd("11800.00"), 6)));

            log.info("Wine seed completed");
        };
    }

    private WineTypeEntity wineType(String name) {
        return wineTypeRepository.findByName(name).orElseGet(() -> wineTypeRepository.save(
                WineTypeEntity.builder()
                        .name(name)
                        .active(true)
                        .build()
        ));
    }

    private GrapeVarietyEntity grapeVariety(String name) {
        return grapeVarietyRepository.findByName(name).orElseGet(() -> grapeVarietyRepository.save(
                GrapeVarietyEntity.builder()
                        .name(name)
                        .active(true)
                        .build()
        ));
    }

    private WineryEntity winery(String name) {
        return wineryRepository.findByName(name).orElseGet(() -> wineryRepository.save(
                WineryEntity.builder()
                        .name(name)
                        .active(true)
                        .build()
        ));
    }

    private DistributorEntity distributor(String name, String contact) {
        return distributorRepository.findByName(name).orElseGet(() -> distributorRepository.save(
                DistributorEntity.builder()
                        .name(name)
                        .contact(contact)
                        .active(true)
                        .build()
        ));
    }

    private void wine(String name,
                      String description,
                      Integer vintage,
                      String region,
                      BigDecimal alcohol,
                      String tastingNotes,
                      WineTypeEntity wineType,
                      WineryEntity winery,
                      DistributorEntity distributor,
                      Set<GrapeVarietyEntity> grapeVarieties,
                      List<VariantSeed> variants) {

        WineEntity wine = wineRepository.findByNameAndWinery(name, winery).orElseGet(() ->
                wineRepository.save(WineEntity.builder()
                        .name(name)
                        .description(description)
                        .vintage(vintage)
                        .region(region)
                        .alcohol(alcohol)
                        .tastingNotes(tastingNotes)
                        .active(true)
                        .wineType(wineType)
                        .winery(winery)
                        .distributor(distributor)
                        .grapeVarieties(new HashSet<>(grapeVarieties))
                        .build())
        );

        for (VariantSeed variantSeed : variants) {
            boolean exists = wineVariantRepository
                    .findByWineAndAttribute(wine, variantSeed.attribute())
                    .isPresent();

            if (exists) {
                continue;
            }

            wineVariantRepository.save(WineVariantEntity.builder()
                    .attribute(variantSeed.attribute())
                    .price(variantSeed.price())
                    .stock(variantSeed.stock())
                    .wholesalePrice(variantSeed.wholesalePrice())
                    .wholesaleMinQty(variantSeed.wholesaleMinQty())
                    .active(true)
                    .wine(wine)
                    .build());
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record VariantSeed(String attribute,
                               BigDecimal price,
                               Integer stock,
                               BigDecimal wholesalePrice,
                               Integer wholesaleMinQty) {
    }
}
