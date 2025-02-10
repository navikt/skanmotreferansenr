package no.nav.skanmotreferansenr.domain;

import no.nav.skanmotreferansenr.exceptions.functional.InvalidMetadataException;
import org.junit.jupiter.api.Test;

import static no.nav.skanmotreferansenr.PostboksReferansenrTestObjects.BATCH_NAVN;
import static no.nav.skanmotreferansenr.PostboksReferansenrTestObjects.DATO_MOTTATT;
import static no.nav.skanmotreferansenr.PostboksReferansenrTestObjects.ENDORSERNR;
import static no.nav.skanmotreferansenr.PostboksReferansenrTestObjects.MOTTAKSKANAL;
import static no.nav.skanmotreferansenr.PostboksReferansenrTestObjects.REFERANSENR;
import static no.nav.skanmotreferansenr.PostboksReferansenrTestObjects.REFERANSENR_WITH_CHECKSUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalpostTest {

    @Test
    void shouldGetReferansenummerWithoutChecksum() {
        final Journalpost journalpost = Journalpost.builder()
                .mottakskanal(MOTTAKSKANAL)
                .datoMottatt(DATO_MOTTATT)
                .batchnavn(BATCH_NAVN)
                .endorsernr(ENDORSERNR)
                .referansenummer(REFERANSENR_WITH_CHECKSUM)
                .build();
        assertThat(journalpost.getReferansenummerWithoutChecksum()).isEqualTo(REFERANSENR);
    }

    @Test
    void shouldThrowExceptionWhenReferansenummerUgyldig() {
        final Journalpost journalpost = Journalpost.builder()
                .mottakskanal(MOTTAKSKANAL)
                .datoMottatt(DATO_MOTTATT)
                .batchnavn(BATCH_NAVN)
                .endorsernr(ENDORSERNR)
                .referansenummer("1111111111111")
                .build();
        assertThrows(InvalidMetadataException.class, journalpost::getReferansenummerWithoutChecksum);
    }
}