package no.nav.skanmotreferansenr.consumer.journalpostapi;

import lombok.extern.slf4j.Slf4j;
import no.nav.skanmotreferansenr.consumer.foersteside.data.FoerstesideMetadata;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.DokumentInfo;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LeggTilLogiskVedleggResponse;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.LogiskVedleggRequest;
import no.nav.skanmotreferansenr.consumer.journalpostapi.data.OpprettJournalpostResponse;
import no.nav.skanmotreferansenr.exceptions.functional.AbstractSkanmotreferansenrFunctionalException;
import no.nav.skanmotreferansenr.exceptions.technical.AbstractSkanmotreferansenrTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class LeggTilLogiskVedleggService {

	private final JournalpostConsumer journalpostConsumer;

	@Autowired
	public LeggTilLogiskVedleggService(JournalpostConsumer journalpostConsumer) {
		this.journalpostConsumer = journalpostConsumer;
	}

	public List<LeggTilLogiskVedleggResponse> leggTilLogiskVedlegg(OpprettJournalpostResponse opprettJournalpostResponse, FoerstesideMetadata foerstesideMetadata) {
		if (opprettJournalpostResponse == null || foerstesideMetadata == null) {
			return new ArrayList<>();
		}
		if (opprettJournalpostResponse.getDokumenter().isEmpty() || foerstesideMetadata.getVedleggsliste() == null) {
			return new ArrayList<>();
		}
		final List<String> dokumentInfoIds = opprettJournalpostResponse.getDokumenter().stream().map(DokumentInfo::dokumentInfoId)
				.toList();
		List<LogiskVedleggRequest> logiskVedleggRequests = IntStream.range(0, dokumentInfoIds.size())
				.mapToObj(i -> new LogiskVedleggRequest(
						dokumentInfoIds.get(i),
						foerstesideMetadata.getVedleggsliste().get(i)))
				.toList();

		return logiskVedleggRequests.stream()
				.map(request -> leggTilLogiskVedlegg(request.dokumentInfoId(), request.tittel()))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private LeggTilLogiskVedleggResponse leggTilLogiskVedlegg(String dokumentInfoId, String tittel) {
		log.info("Skanmotreferansenr legger til logisk vedlegg, dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel);
		LeggTilLogiskVedleggRequest request = LeggTilLogiskVedleggRequest.builder()
				.tittel(tittel)
				.build();
		try {
			return journalpostConsumer.leggTilLogiskVedlegg(request, dokumentInfoId);
		} catch (AbstractSkanmotreferansenrFunctionalException e) {
			log.warn("Skanmotreferansenr feilet funksjonelt med lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel, e);
			return null;
		} catch (AbstractSkanmotreferansenrTechnicalException e) {
			log.warn("Skanmotreferansenr feilet teknisk med lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel, e);
			return null;
		} catch (Exception e) {
			log.warn("Skanmotreferansenr feilet med ukjent feil ved lagring av logisk vedlegg dokumentInfoId={}, dokumentTittel={}", dokumentInfoId, tittel, e);
			return null;
		}
	}
}
