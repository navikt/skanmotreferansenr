package no.nav.skanmotreferansenr.util;

import org.apache.camel.Handler;
import org.apache.camel.Predicate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Helligdager {
    private Helligdager() {
        //Privat constructor for å hindre instanser.
    }

    public static boolean erHelligdag(LocalDate dato) {
        List<LocalDate> helligdager = finnBevegeligeHelligdagerUtenHelgPerAAr(dato.getYear());

        return helligdager.stream().anyMatch(dato::isEqual);

    }

    static List<LocalDate> finnBevegeligeHelligdagerUtenHelgPerAAr(int aar) {
        List<LocalDate> bevegeligeHelligdager = new ArrayList<>();

        // legger til de satte helligdagene
        bevegeligeHelligdager.add(LocalDate.of(aar, 1, 1));
        bevegeligeHelligdager.add(LocalDate.of(aar, 5, 1));
        bevegeligeHelligdager.add(LocalDate.of(aar, 5, 17));
        bevegeligeHelligdager.add(LocalDate.of(aar, 12, 25));
        bevegeligeHelligdager.add(LocalDate.of(aar, 12, 26));

        // regner ut påskedag
        LocalDate paaskedag = utledPaaskedag(aar);

        // søndag før påske; Palmesøndag
        bevegeligeHelligdager.add(paaskedag.minusDays(7));

        // torsdag før påske; Skjærtorsdag
        bevegeligeHelligdager.add(paaskedag.minusDays(3));

        // fredag før påske; Langfredag
        bevegeligeHelligdager.add(paaskedag.minusDays(2));

        // 1.påskedag
        bevegeligeHelligdager.add(paaskedag);

        // 2.påskedag
        bevegeligeHelligdager.add(paaskedag.plusDays(1));

        // Kristi Himmelfartsdag
        bevegeligeHelligdager.add(paaskedag.plusDays(39));

        // 1.pinsedag
        bevegeligeHelligdager.add(paaskedag.plusDays(49));

        // 2.pinsedag
        bevegeligeHelligdager.add(paaskedag.plusDays(50));

        return bevegeligeHelligdager;
    }

    private static LocalDate utledPaaskedag(int aar) {
        int a = aar % 19;
        int b = aar / 100;
        int c = aar % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = ((19 * a) + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + (2 * e) + (2 * i) - h - k) % 7;
        int m = (a + (11 * h) + (22 * l)) / 451;
        int n = (h + l - (7 * m) + 114) / 31; // Tallet på måneden
        int p = (h + l - (7 * m) + 114) % 31; // Tallet på dagen

        return LocalDate.of(aar, n, p + 1);
    }
}