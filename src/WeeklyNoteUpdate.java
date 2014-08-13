import java.io.BufferedWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.joda.time.DateTime;

import com.bullhorn.apiservice.ApiService;
import com.bullhorn.apiservice.ApiService_Service;
import com.bullhorn.apiservice.query.DtoQuery;
import com.bullhorn.apiservice.result.ApiFindResult;
import com.bullhorn.apiservice.result.ApiGetNoteReferencesResult;
import com.bullhorn.apiservice.result.ApiQueryResult;
import com.bullhorn.apiservice.result.ApiSaveResult;
import com.bullhorn.apiservice.result.ApiStartSessionResult;
import com.bullhorn.apiservice.struct.EntityNameIdPair;
import com.bullhorn.entity.candidate.CandidateDto;
import com.bullhorn.entity.note.NoteDto;

public class WeeklyNoteUpdate {

	private static final QName SERVICE_NAME = new QName(
			"http://apiservice.bullhorn.com/", "ApiService");
	private static final String WSDL_URL = "https://api.bullhornstaffing.com/webservices-2.5/?wsdl";
	private static final String API_USER = "314e.API";
	private static final String API_PASSWD = "314e.com";
	private static final String API_KEY = "F5EA5835-9A07-81A4-448468FE85A30556";

	public static void main(String[] args) {
	try {
		List<String> noteids = null;
		final Path noteFile = FileSystems.getDefault().getPath("candidates.list");
		if (Files.exists(noteFile)) {
			noteids = Files.readAllLines(noteFile);
		} else {
			noteids = new ArrayList<>();
		}

		final URL serviceUrl = new URL(
				ApiService_Service.class.getResource("."), WSDL_URL);

		final ApiService apiService = new ApiService_Service(serviceUrl,
				SERVICE_NAME).getApiServicePort();

		final ApiStartSessionResult startSession = apiService.startSession(
				API_USER, API_PASSWD, API_KEY);

		String session = startSession.getSession();

		ApiFindResult candidateResults;
		CandidateDto candidate;

		// Get the last week from current date

		DateTime dt = new DateTime();
		DateTime lwd = dt.minusDays(9);
		String lastWeek = lwd.toLocalDate().toString("MM/dd/yyyy");

		// Get all the notes from last week
		DtoQuery query = new DtoQuery();
		query.setEntityName("Note");
		query.setMaxResults(750000000);
		query.setWhere("dateAdded>=" + "'" + lastWeek + "'" + " " + "AND" + " "
				+ "isDeleted=0" + " " + "AND" + " " + "(action='Outbound Call'"
				+ " " + "OR" + " " + "action='Inbound Call'" + " " + "OR" + " "
				+ "action='Prescreened'" + " " + "OR" + " "
				+ "action='Interview')");
		query.getOrderBys().add("dateAdded asc");

		ApiQueryResult qResult = apiService.query(session, query);
		session = qResult.getSession();
		System.out.println(qResult.getIds());
		System.out.println(qResult.getIds().size());
		ApiFindResult noteResults = apiService.find(session, "Note", qResult
				.getIds().get(0));

		int noteId = (int) qResult.getIds().get(0);

		NoteDto weeklyNote = (NoteDto) noteResults.getDto();

		DtoQuery query1 = new DtoQuery();
		query1.setEntityName("Candidate");
		query1.setMaxResults(1);
		query1.setWhere("userID='52452'");

		ApiQueryResult candConf = apiService.query(session, query1);

		ApiSaveResult updated;
		
		final BufferedWriter writer = Files.newBufferedWriter(noteFile, Charset.forName("US-ASCII"), StandardOpenOption.CREATE);
		// Run through the notes
		for (int index = 0; index < qResult.getIds().size(); index++) {

			noteId = (int) qResult.getIds().get(index);

			noteResults = apiService.find(session, "Note", noteId);
			session = noteResults.getSession();

			ApiGetNoteReferencesResult reference = apiService
					.getNoteReferences(session, noteId);
			session = reference.getSession();

			weeklyNote = (NoteDto) noteResults.getDto();

			System.out.println(noteId + " " + (index + 1));
			System.out.println(weeklyNote.getDateAdded());
			
			if (noteids.contains(String.valueOf(noteId))) {
				continue;
			}

			// Go through the references of each note
			for (EntityNameIdPair pair : reference.getEntityList()) {

				// Perform query search to see if person id is candidate or not.
				query1 = new DtoQuery();
				query1.setEntityName("Candidate");
				query1.setMaxResults(1);
				query1.setWhere("userID=" + pair.getEntityId().toString());

				candConf = apiService.query(session, query1);
				session = candConf.getSession();

				if (candConf.getIds().size() == 1
						&& pair.getEntityName().equals("Person")) {
					
					System.out.println(noteids);

					System.out.println(pair.getEntityId().toString() + " "
							+ pair.getEntityName().toString());

					candidateResults = apiService.find(session, "Candidate",
							candConf.getIds().get(0));
					session = candidateResults.getSession();

					candidate = (CandidateDto) candidateResults.getDto();

					candidate.setCustomDate2(weeklyNote.getDateAdded());
					System.out.println(candidate.getCustomDate2());

					updated = apiService.save(session, candidate);
					session = updated.getSession();
					
					

				}

			}

			System.out.println(" ");
			writer.write(String.valueOf(noteId));
			writer.newLine();
			writer.flush();
			
		}
		
		writer.close();
		//Delete the file candidate.list
		Files.deleteIfExists(noteFile);
	}catch (final Exception ex) {
		System.exit(10);
	}

	}

}
