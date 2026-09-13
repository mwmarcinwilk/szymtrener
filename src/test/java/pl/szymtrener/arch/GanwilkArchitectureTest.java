package pl.szymtrener.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * Reguły architektury — wygenerowane przez `vault upgrade` (2026-09-01).
 *
 * Wszystkie reguły są ZAMROŻONE (FreezingArchRule): pierwszy przebieg zapisuje naruszenia
 * zastane w katalogu archunit_store/, a test przechodzi. Od tej pory build wywala się
 * wyłącznie na NOWYCH naruszeniach. Dług architektoniczny nie rośnie, a nikt nie musi
 * najpierw sprzątać całego projektu.
 *
 * Żeby uznać naruszenie za naprawione: usuń odpowiedni wpis z archunit_store/ (albo cały
 * katalog, żeby przeliczyć stan od zera). Katalog archunit_store/ NALEŻY commitować —
 * to jest linia bazowa wspólna dla całego zespołu.
 *
 * Adnotacje wskazywane po nazwie tekstowej, nie po klasie, żeby test kompilował się także
 * w projektach bez Spring Web albo bez Spring TX.
 */
class GanwilkArchitectureTest {

    private static final String BASE = "pl.szymtrener";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    /** Cykle między pakietami zacierają granice modułów i blokują późniejszy podział. */
    @Test
    void brak_cykli_miedzy_pakietami() {
        // allowEmptyShould: projekt bez podpakietów (klasy płasko w pakiecie bazowym)
        // nie ma plastrów do sprawdzenia, a ArchUnit domyślnie traktuje to jako błąd.
        ArchRule rule = slices().matching(BASE + ".(*)..").should().beFreeOfCycles()
                .allowEmptyShould(true);
        freeze(rule).check(classes);
    }

    /** Kontroler sięgający wprost do repozytorium omija logikę i walidację z serwisu. */
    @Test
    void kontrolery_nie_siegaja_wprost_do_repozytoriow() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..controller..", "..web..", "..api..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .allowEmptyShould(true);
        freeze(rule).check(classes);
    }

    /** @Transactional na kontrolerze rozciąga transakcję na renderowanie widoku. */
    @Test
    void transakcje_nie_na_kontrolerach() {
        ArchRule rule = noClasses()
                .that().areAnnotatedWith("org.springframework.stereotype.Controller")
                .or().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
                .allowEmptyShould(true);
        freeze(rule).check(classes);
    }

    /** Zależność serwisu od warstwy web odwraca kierunek i uniemożliwia testowanie bez HTTP. */
    @Test
    void serwisy_nie_zaleza_od_warstwy_web() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..service..", "..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..web..")
                .allowEmptyShould(true);
        freeze(rule).check(classes);
    }
}
