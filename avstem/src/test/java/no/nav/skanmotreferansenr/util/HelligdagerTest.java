package no.nav.skanmotreferansenr.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.skanmotreferansenr.util.Helligdager.erHelligdag;
import static org.junit.jupiter.api.Assertions.*;

class HelligdagerTest {

	@Test
	void fasteHelligdagerTest() {
		assertTrue(erHelligdag(LocalDate.of(2025, 1, 1)));
		assertFalse(erHelligdag(LocalDate.of(2025, 1, 2)));
		assertFalse(erHelligdag(LocalDate.of(2025, 1, 3)));
		assertFalse(erHelligdag(LocalDate.of(2025, 1, 4)));
		assertFalse(erHelligdag(LocalDate.of(2025, 1, 5)));

		assertTrue(erHelligdag(LocalDate.of(2025, 5, 1)));

		assertFalse(erHelligdag(LocalDate.of(2025, 5, 16)));
		assertTrue(erHelligdag(LocalDate.of(2025, 5, 17)));
		assertFalse(erHelligdag(LocalDate.of(2025, 5, 18)));
		assertTrue(erHelligdag(LocalDate.of(2035, 5, 17)));
		assertTrue(erHelligdag(LocalDate.of(2015, 5, 17)));

		assertTrue(erHelligdag(LocalDate.of(2022, 12, 25)));
		assertTrue(erHelligdag(LocalDate.of(2023, 12, 25)));
		assertTrue(erHelligdag(LocalDate.of(2024, 12, 25)));
		assertTrue(erHelligdag(LocalDate.of(2025, 12, 25)));
		assertTrue(erHelligdag(LocalDate.of(2025, 12, 26)));
		assertTrue(erHelligdag(LocalDate.of(2026, 12, 26)));
		assertTrue(erHelligdag(LocalDate.of(2027, 12, 26)));
		assertTrue(erHelligdag(LocalDate.of(2028, 12, 26)));

	}

	@Test
	void bevegeligeHelligdagerTest() {
		assertTrue(erHelligdag(LocalDate.of(2025, 4, 17)));
		assertTrue(erHelligdag(LocalDate.of(2025, 4, 18)));
		assertTrue(erHelligdag(LocalDate.of(2025, 4, 21)));
		assertTrue(erHelligdag(LocalDate.of(2025, 5, 29)));
		assertFalse(erHelligdag(LocalDate.of(2025, 5, 30)));

		assertTrue(erHelligdag(LocalDate.of(2028, 5, 25)));
		assertTrue(erHelligdag(LocalDate.of(2028, 6, 5)));
		assertTrue(erHelligdag(LocalDate.of(2028, 4, 17)));

		assertTrue(erHelligdag(LocalDate.of(2018, 4, 2)));
		assertTrue(erHelligdag(LocalDate.of(2018, 3, 30)));
		assertTrue(erHelligdag(LocalDate.of(2018, 5, 21)));
	}
}