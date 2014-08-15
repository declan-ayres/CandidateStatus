import java.io.BufferedWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.bullhorn.apiservice.ApiService;
import com.bullhorn.apiservice.ApiService_Service;
import com.bullhorn.apiservice.edithistory.ApiEditHistory;
import com.bullhorn.apiservice.edithistory.ApiEditHistoryFieldChange;
import com.bullhorn.apiservice.query.DtoQuery;
import com.bullhorn.apiservice.result.ApiFindResult;
import com.bullhorn.apiservice.result.ApiGetEditHistoryResult;
import com.bullhorn.apiservice.result.ApiQueryResult;
import com.bullhorn.apiservice.result.ApiStartSessionResult;
import com.bullhorn.entity.candidate.CandidateDto;
import com.bullhorn.entity.user.CorporateUserDto;

/**
 * 
 * @author declan.ayres
 *
 */
public class CandidateChanges {
	private static final QName SERVICE_NAME = new QName(
			"http://apiservice.bullhorn.com/", "ApiService");
	private static final String WSDL_URL = "https://api.bullhornstaffing.com/webservices-2.5/?wsdl";
	private static final String API_USER = "314e.API";
	private static final String API_PASSWD = "314e.com";
	private static final String API_KEY = "F5EA5835-9A07-81A4-448468FE85A30556";
	private static Properties properties = new Properties();

	public static void main(String[] args) {
		try {
		
		
		
		List<String> candidateids = null;
		final Path candidatefile = FileSystems.getDefault().getPath("candidate.list");
		if (Files.exists(candidatefile)) {
			candidateids = Files.readAllLines(candidatefile);
		} else {
			candidateids = new ArrayList<>();
		}
		
		properties.load(CandidateChanges.class.getResourceAsStream("/bullhorn.properties"));


		final URL serviceUrl = new URL(
				ApiService_Service.class.getResource("."), WSDL_URL);

		final ApiService apiService = new ApiService_Service(serviceUrl,
				SERVICE_NAME).getApiServicePort();

		final ApiStartSessionResult startSession = apiService.startSession(
				API_USER, API_PASSWD, API_KEY);

		String session = startSession.getSession();

		// Get all candidates
		DtoQuery query2 = new DtoQuery();
		query2.setEntityName("Candidate");
		query2.setMaxResults(300000);
		query2.getOrderBys().add("id asc");
		query2.setWhere("isDeleted=0" + " " + "AND NOT" + " "
				+ "status = 'archive'" + " " + "AND" + " " + "customText20!=''");

		ApiQueryResult qResult2 = apiService.query(session, query2);
		ApiFindResult candidateResults = apiService.find(session, "Candidate",
				qResult2.getIds().get(0));
		System.out.println(qResult2.getIds());
		System.out.println(qResult2.getIds().size());

		CandidateDto candidate = (CandidateDto) candidateResults.getDto();

		// Prep for email message
		final String username = "bullhorn@314ecorp.com";
		final String password = "Kaiser123";

		Properties props = new Properties();
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session mailSession = Session.getInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(username, password);
					}
				});

		String compiledChanges = new String();

		compiledChanges = "";

		String temp = "";

		compiledChanges += temp;

		XMLGregorianCalendar da;

		org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat
				.forPattern("EEEE MMMM dd, yyyy");
		DateTime date;

		DateTime currDate = new DateTime();

		System.out.println(currDate);

		XMLGregorianCalendar startDate = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(
						currDate.minusDays(9).toGregorianCalendar());

		XMLGregorianCalendar endDate = DatatypeFactory.newInstance()
				.newXMLGregorianCalendar(currDate.toGregorianCalendar());

		int day;
		int month;
		int year;
		int hour;
		int minute;

		ApiGetEditHistoryResult history;
		List<ApiEditHistory> histObjects;
		
		final BufferedWriter writer = Files.newBufferedWriter(candidatefile, Charset.forName("US-ASCII"),
				StandardOpenOption.CREATE);

		// go through candidates
		search: for (int index = 0; index < qResult2.getIds().size(); index++) {

			candidateResults = apiService.find(session, "Candidate", qResult2
					.getIds().get(index));
			session = candidateResults.getSession();
			candidate = (CandidateDto) candidateResults.getDto();
			
			if (candidateids.contains(qResult2.getIds().get(index).toString())) {
				continue;
			}

			//Special case for candidate 5665 because of invalid character
			if (qResult2.getIds().get(index).equals(5665)) {

				history = apiService.getEditHistoryByDates(session,
						"Candidate", qResult2.getIds().get(index), startDate,
						endDate);
				histObjects = history.getEditHistories();
				session = history.getSession();
				System.out.println(qResult2.getIds().get(index));

			} else {

				history = apiService.getEditHistory(session, "Candidate",
						qResult2.getIds().get(index));
				histObjects = history.getEditHistories();
				session = history.getSession();

			}

			// go through each candidates edit history
			for (int i = 0; i < histObjects.size(); i++) {

				da = histObjects.get(0).getDateAdded();

				day = da.getDay();
				month = da.getMonth();
				year = da.getYear();
				hour = da.getHour();
				minute = da.getMinute();

				date = new DateTime(year, month, day, hour, minute, 0);

				if (date.isBefore(currDate.minusMonths(1).getMillis())) {
					System.out.println((index + 1));
					continue search;
				}

				List<ApiEditHistoryFieldChange> histChanges = histObjects
						.get(i).getFieldChanges();

				// run through each change if on the same tab
				for (ApiEditHistoryFieldChange item : histChanges) {

					if (item.getColumnName().equals("customText20")) {

						da = histObjects.get(i).getDateAdded();

						day = da.getDay();
						month = da.getMonth();
						year = da.getYear();
						hour = da.getHour();
						minute = da.getMinute();

						date = new DateTime(year, month, day, hour, minute, 0);

						if (date.isAfter(currDate.minusDays(7).getMillis())) {

							ApiFindResult userResult = apiService.find(session,
									"CorporateUser", histObjects.get(i)
											.getModifyingPersonId());
							CorporateUserDto user = (CorporateUserDto) userResult
									.getDto();

							if (compiledChanges.contains(candidate.getName())) {

								temp = "<span style='font-size:13px;color:rgb(102,102,102);'>"
										+ " "
										+ "<span style=font-size:13px;>"
										+ "<b>"
										+ properties.getProperty("field") //candidate field that was changed
										+ "</b>"
										+ "</span>"
										+ ": Changed from"
										+ " "
										+ "'"
										+ "<i>"
										+ "<strong>"
										+ item.getOldValue()
										+ "</strong>"
										+ "</i>"
										+ "'"
										+ " "
										+ "to"
										+ " "
										+ "'"
										+ "<i>"
										+ "<strong>"
										+ item.getNewValue()
										+ "</strong>"
										+ "</i>"
										+ "'"
										+ " "
										+ "by"
										+ " "
										+ "<b>"
										+ user.getName()
										+ "</b>"
										+ " "
										+ "on"
										+ " "
										+ date.minusHours(3)
												.toString(formatter)
										+ "</span>";

								compiledChanges += temp + "<br>";
							} else {

								// text formatting
								temp = "<span style='font-size:13px;color:rgb(102,102,102);'>"
										+ "<strong><a href=https://cls5.bullhornstaffing.com/BullhornStaffing/OpenWindow.cfm?Entity=Candidate&id="
										+ candidate.getUserID()
										+ " style='color:#48C3B1;font-family:arial,sans-serif;font-size:16px;text-decoration:none;'>"
										+ candidate.getName()
										+ "</a></strong><br>"
										+ " "
										+ "<span style=font-size:13px;>"
										+ "<b>"
										+ properties.getProperty("field") //the candidate field that was changed
										+ "</b>"
										+ "</span>"
										+ ": Changed from"
										+ " "
										+ "'"
										+ "<i>"
										+ "<strong>"
										+ item.getOldValue()
										+ "</strong>"
										+ "</i>"
										+ "'"
										+ " "
										+ "to"
										+ " "
										+ "'"
										+ "<i>"
										+ "<strong>"
										+ item.getNewValue()
										+ "</strong>"
										+ "</i>"
										+ "'"
										+ " "
										+ "by"
										+ " "
										+ "<b>"
										+ user.getName()
										+ "</b>"
										+ " "
										+ "on"
										+ " "
										+ date.minusHours(3)
												.toString(formatter)
										+ "</span>";

								compiledChanges += "\n\n" + temp + "<br>";
							}

							System.out.println(candidate.getName() + " "
									+ (index + 1));
							System.out.println(temp);
							System.out.println(compiledChanges);
							
							compiledChanges += "<br>";
						}

						System.out.println("\n\n\n\n\n");

					}

				}

			}

			
			System.out.println((index + 1));
			writer.write(qResult2.getIds().get(index).toString());
			writer.newLine();
			writer.flush();

		}
		
		writer.close();
		// Delete the file candidate.list
		Files.deleteIfExists(candidatefile);

		
		// send email
		try {

			Message message = new MimeMessage(mailSession);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(properties.getProperty("recipient")));
			message.setSubject(properties.getProperty("subject"));
			message.setText(compiledChanges);
			message.setContent(compiledChanges, "text/html; charset=utf-8");
			
			if (!compiledChanges.equals("")) {
			Transport.send(message);
			}

			System.out.println("Done");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
		} catch (final Exception ex) {
			System.exit(10);
		}

	}

}