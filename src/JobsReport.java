import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

import com.bullhorn.apiservice.ApiService;
import com.bullhorn.apiservice.ApiService_Service;
import com.bullhorn.apiservice.edithistory.ApiEditHistory;
import com.bullhorn.apiservice.edithistory.ApiEditHistoryFieldChange;
import com.bullhorn.apiservice.query.DtoQuery;
import com.bullhorn.apiservice.result.ApiFindResult;
import com.bullhorn.apiservice.result.ApiGetAssociationIdsResult;
import com.bullhorn.apiservice.result.ApiGetEditHistoryResult;
import com.bullhorn.apiservice.result.ApiGetEntityNotesResult;
import com.bullhorn.apiservice.result.ApiQueryResult;
import com.bullhorn.apiservice.result.ApiStartSessionResult;
import com.bullhorn.entity.appointment.AppointmentDto;
import com.bullhorn.entity.candidate.SendoutDto;
import com.bullhorn.entity.job.JobOrderDto;
import com.bullhorn.entity.job.JobSubmissionDto;
import com.bullhorn.entity.job.PlacementDto;
import com.bullhorn.entity.note.NoteDto;
import com.bullhorn.entity.user.CorporateUserDto;

public class JobsReport {

	private static final QName SERVICE_NAME = new QName("http://apiservice.bullhorn.com/", "ApiService");
	private static final String WSDL_URL = "https://api.bullhornstaffing.com/webservices-2.5/?wsdl";
	private static final String API_USER = "314e.API";
	private static final String API_PASSWD = "314e.com";
	private static final String API_KEY = "F5EA5835-9A07-81A4-448468FE85A30556";

	public static void main(String[] args) throws Exception {

		final URL serviceUrl = new URL(ApiService_Service.class.getResource("."), WSDL_URL);

		final ApiService apiService = new ApiService_Service(serviceUrl, SERVICE_NAME).getApiServicePort();

		final ApiStartSessionResult startSession = apiService.startSession(API_USER, API_PASSWD, API_KEY);

		String session = startSession.getSession();

		// get all recruiters
		DtoQuery distQuery = new DtoQuery();
		distQuery.setEntityName("DistributionList");
		distQuery.setMaxResults(100);
		distQuery.setWhere("name='Recruiters'");

		ApiQueryResult distResults = apiService.query(session, distQuery);

		ApiGetAssociationIdsResult recruiters = apiService.getAssociationIds(session, "DistributionList", distResults
				.getIds().get(0), "members");

		ArrayList<String> recruiterList = new ArrayList<>();
		ArrayList<Integer> recruiterIdList = new ArrayList<>();

		// sort and get active recruiters
		for (int index = 0; index < recruiters.getIds().size(); index++) {
			ApiFindResult findRecruiter = apiService.find(session, "CorporateUser", recruiters.getIds().get(index));
			CorporateUserDto recruiter = (CorporateUserDto) findRecruiter.getDto();
			if (recruiter.isIsDeleted().equals(true) || recruiter.getFirstName().equals("Abhishek")
					|| recruiter.getFirstName().equals("Alok")) {

			} else {
				recruiterList.add(recruiter.getName());
				recruiterIdList.add(recruiter.getUserID());
			}

		}

		System.out.println(recruiterList);

		Date date = new Date();
		GregorianCalendar cal = new GregorianCalendar();
		date.setMonth(date.getMonth() - 2);
		cal.setTime(date);

		// get jobs in the past 6 months
		DtoQuery jobQuery = new DtoQuery();
		jobQuery.setEntityName("JobOrder");
		jobQuery.setMaxResults(100000);
		jobQuery.setWhere("dateAdded>'2/11/2014'");
		jobQuery.getOrderBys().add("dateAdded desc");

		List<List<Object>> jobsList = new ArrayList<List<Object>>();

		ApiQueryResult jobResults = apiService.query(session, jobQuery);
		System.out.println(jobResults.getIds().size());

		// prepare dates for start and end dates for edit history
		Date currDate = new Date();
		GregorianCalendar c1 = new GregorianCalendar();
		c1.setTime(currDate);
		XMLGregorianCalendar endDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c1);

		c1.setTime(currDate);
		XMLGregorianCalendar startDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c1);
		startDate.setHour(0);
		startDate.setMinute(0);

		int count = 0;

		// go through jobs
		for (int index = 0; index < jobResults.getIds().size(); index++) {
			System.out.println(index);
			ApiFindResult findJob = apiService.find(session, "JobOrder", jobResults.getIds().get(index));
			session = findJob.getSession();
			JobOrderDto job = (JobOrderDto) findJob.getDto();
			System.out.println(job.isIsOpen().equals(true));
			if (job.isIsOpen().equals(true)) {
				jobsList.add(new ArrayList<Object>());
				jobsList.get(count).add(job.getJobOrderID());
				if (!job.getCorrelatedCustomText2().equals("")) {
					jobsList.get(count).add(job.getCorrelatedCustomText2());
				} else {
					// see if recruiter removed itself from job
					ApiGetEditHistoryResult jobHistory = apiService.getEditHistoryByDates(session, "JobOrder",
							jobResults.getIds().get(index), startDate, endDate);
					for (ApiEditHistory change : jobHistory.getEditHistories()) {
						for (ApiEditHistoryFieldChange fieldChange : change.getFieldChanges()) {
							if (fieldChange.getColumnName().equals("assignments")) {
								if (jobsList.get(count).size() == 1) {
									jobsList.get(count).add(fieldChange.getOldValue() + fieldChange.getNewValue());
								} else {
									jobsList.get(count).get(1).toString()
											.concat(fieldChange.getOldValue() + fieldChange.getNewValue());
								}
							} else if (jobHistory.getEditHistories().get(jobHistory.getEditHistories().size() - 1)
									.equals(change)
									&& change.getFieldChanges().get(change.getFieldChanges().size() - 1)
											.equals(fieldChange) && jobsList.get(count).size() == 1) {
								jobsList.get(count).add("");
							}
						}
					}
				}
				count++;
			} else {

				// get the job history to see if it was changed from open to
				// closed today
				ApiGetEditHistoryResult jobHistory = apiService.getEditHistoryByDates(session, "JobOrder", jobResults
						.getIds().get(index), startDate, endDate);
				for (ApiEditHistory change : jobHistory.getEditHistories()) {
					for (ApiEditHistoryFieldChange fieldChange : change.getFieldChanges()) {
						if (fieldChange.getColumnName().equals("isOpen")) {
							if (fieldChange.getOldValue().equals("Open") && !job.getCorrelatedCustomText2().equals("")
									&& count >= jobsList.size()) {
								jobsList.add(new ArrayList<Object>());
								jobsList.get(count).add(0, job.getJobOrderID());
								jobsList.get(count).add(job.getCorrelatedCustomText2());
							} else if (fieldChange.getOldValue().equals("Open")
									&& job.getCorrelatedCustomText2().equals("") && count >= jobsList.size()) {
								jobsList.add(new ArrayList<Object>());
								jobsList.get(count).add(0, job.getJobOrderID());
							} else if (fieldChange.getOldValue().equals("Open") && count < jobsList.size()
									&& jobsList.get(count).get(0).equals("")) {
								jobsList.get(count).set(0, job.getJobOrderID());
							}
						} else if (fieldChange.getColumnName().equals("correlatedCustomText2")) {
							if (count >= jobsList.size()) {
								jobsList.add(new ArrayList<Object>());
								jobsList.get(count).add("");
								jobsList.get(count).add(fieldChange.getOldValue() + fieldChange.getNewValue());
							} else {
								if (jobsList.get(count).size() == 1) {
									jobsList.get(count).add(fieldChange.getOldValue() + fieldChange.getNewValue());
								} else {
									jobsList.get(count).get(1).toString()
											.concat(fieldChange.getOldValue() + fieldChange.getNewValue());
								}
							}
						}
						if (jobHistory.getEditHistories().get(jobHistory.getEditHistories().size() - 1).equals(change)
								&& change.getFieldChanges().get(change.getFieldChanges().size() - 1)
										.equals(fieldChange) && count < jobsList.size()) {
							count++;
						}
					}
				}
			}

		}

		// number of jobs list for each recruiter
		List<Integer> assocList = new ArrayList<Integer>();
		// notes action list for each job for each recruiter

		// total numbers of job info for each recruiter

		// the text for the email
		String text = "";
		String noteString = "";
		String totalNoteString = "";
		String compiledText = "";
		String intSubsString = "";
		String clientSubsString = "";
		String interviewString = "";
		String placementString = "";
		String totalIntSubsString = "";
		String totalClientSubsString = "";
		String totalInterviewString = "";
		String totalPlacementString = "";
		String statusString = "";
		String openClosedString = "";
		String recruiterString = "";
		String titleString = "";

		System.out.println(jobsList);
		System.out.println(jobsList.size());
		int recruiterCount = 0;

		// go through recruiters
		for (String recruiter : recruiterList) {

			assocList.clear();
			compiledText = compiledText
					+ "\n<strong style='color:rgb(98,99,102);font-family:Helvetica,Arial,sans-serif;font-size:12px;line-height:15px'><font color=48c3b1 size=4>"
					+ recruiter + "</font></strong>";

			List<String> totalNoteActionList = new ArrayList<String>();
			totalNoteString = "";
			totalIntSubsString = "";
			totalClientSubsString = "";
			totalInterviewString = "";
			totalPlacementString = "";
			int totalNumberOfPlacements = 0;
			int totalNumberOfInternalSubs = 0;
			int totalNumberOfClientSubs = 0;
			int totalNumberOfInterviews = 0;

			for (List<Object> pair : jobsList) {

				if (pair.size() > 1 && !pair.get(0).equals("")) {
					if (pair.get(1).toString().contains(recruiter)) {
						assocList.add((Integer) pair.get(0));
					}
				}
			}

			System.out.println(recruiter + ": " + assocList);
			for (Integer job : assocList) {

				List<String> noteActionList = new ArrayList<String>();
				noteString = "";
				text = "";
				intSubsString = "";
				clientSubsString = "";
				interviewString = "";
				placementString = "";
				statusString = "";
				openClosedString = "";
				recruiterString = "";
				titleString = "";
				int numberOfPlacements = 0;
				int numberOfInternalSubs = 0;
				int numberOfClientSubs = 0;
				int numberOfInterviews = 0;

				ApiFindResult jobTemp = apiService.find(session, "JobOrder", job);
				JobOrderDto jobInfo = (JobOrderDto) jobTemp.getDto();
				titleString = "	<b><font color=#666666>" + jobInfo.getTitle() + "</font></b>";
				statusString = "	<br><font color=#666666><b>Current Status:</b> " + jobInfo.getStatus() + " ("
						+ (jobInfo.isIsOpen() ? "Open)" : "Closed)") + "</font>";

				ApiGetEditHistoryResult jobHistory = apiService.getEditHistoryByDates(session, "JobOrder", job,
						startDate, endDate);
				for (ApiEditHistory change : jobHistory.getEditHistories()) {
					for (ApiEditHistoryFieldChange fieldChange : change.getFieldChanges()) {
						if (fieldChange.getColumnName().equals("isOpen")) {
							openClosedString += "	<br><font color=#666666>" + fieldChange.getOldValue() + " to "
									+ fieldChange.getNewValue() + " at " + (change.getDateAdded().getHour() - 3) + ":"
									+ change.getDateAdded().getMinute() + "</font>";

						}

						if (fieldChange.getColumnName().equals("assignedUsers")) {
							recruiterString += "	<br><font color=#666666>" + fieldChange.getOldValue() + " to "
									+ fieldChange.getNewValue() + " at " + (change.getDateAdded().getHour() - 3) + ":"
									+ change.getDateAdded().getMinute() + "</font>";
						}

					}
				}
				// find notes for job
				ApiGetAssociationIdsResult jobAssociations = apiService.getAssociationIds(session, "JobOrder", job,
						"notes");
				session = jobAssociations.getSession();
				for (Object assocNote : jobAssociations.getIds()) {
					ApiFindResult noteResults = apiService.find(session, "Note", assocNote);
					NoteDto note = (NoteDto) noteResults.getDto();
					if (note.getDateAdded().toGregorianCalendar().after(startDate.toGregorianCalendar())) {
						if (note.getCommentingPersonID().equals(recruiterIdList.get(recruiterCount))) {
						noteActionList.add(note.getAction());
						}
						//totalNoteActionList.add(note.getAction());

					}

				}
				// find interviews for job
				jobAssociations = apiService.getAssociationIds(session, "JobOrder", job, "interviews");
				for (Object assocInterview : jobAssociations.getIds()) {
					ApiFindResult interviewResults = apiService.find(session, "Appointment", assocInterview);
					AppointmentDto interview = (AppointmentDto) interviewResults.getDto();
					if (interview.getDateAdded().toGregorianCalendar().after(startDate.toGregorianCalendar())) {
						numberOfInterviews++;
						//totalNumberOfInterviews++;
						interviewString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Interviews:</b> "
								+ numberOfInterviews + "</font>";
						//totalInterviewString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Interviews:</b> "
								//+ totalNumberOfInterviews + "</font>";
					}
				}
				// find placements for job
				jobAssociations = apiService.getAssociationIds(session, "JobOrder", job, "placements");
				for (Object assocPlacement : jobAssociations.getIds()) {
					ApiFindResult placementResults = apiService.find(session, "Placement", assocPlacement);
					PlacementDto placement = (PlacementDto) placementResults.getDto();
					if (placement.getDateAdded().toGregorianCalendar().after(startDate.toGregorianCalendar())) {
						numberOfPlacements++;
						//totalNumberOfPlacements++;
						placementString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Placements:</b> "
								+ numberOfPlacements + "</font>";
						//totalPlacementString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Placements:</b> "
								//+ totalNumberOfPlacements + "</font>";
					}
				}
				// find sendouts/client submissions for job
				jobAssociations = apiService.getAssociationIds(session, "JobOrder", job, "sendouts");
				for (Object assocSendout : jobAssociations.getIds()) {
					ApiFindResult sendoutResults = apiService.find(session, "Sendout", assocSendout);
					SendoutDto sendout = (SendoutDto) sendoutResults.getDto();
					if (sendout.getDateAdded().toGregorianCalendar().after(startDate.toGregorianCalendar())) {
						sendout.getUserID();
						CorporateUserDto match = (CorporateUserDto) apiService.find(session, "CorporateUser", sendout.getUserID()).getDto();
						if (match.getName().contains(recruiter)) {
						numberOfClientSubs++;
						
						//totalNumberOfClientSubs++;
						clientSubsString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Client Submissions:</b> "
								+ numberOfClientSubs + "</font>";
						//totalClientSubsString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Client Submissions:</b> "
								//+ totalNumberOfClientSubs + "</font>";
						}
					}

				}
				// find job submissions/ internal submissions for job
				jobAssociations = apiService.getAssociationIds(session, "JobOrder", job, "submissions");
				for (Object assocSubmission : jobAssociations.getIds()) {
					ApiFindResult submissionResults = apiService.find(session, "JobSubmission", assocSubmission);
					JobSubmissionDto submission = (JobSubmissionDto) submissionResults.getDto();
					if (submission.getDateAdded().toGregorianCalendar().after(startDate.toGregorianCalendar())) {
						CorporateUserDto match = (CorporateUserDto) apiService.find(session, "CorporateUser", submission.getSendingUserID()).getDto();
						if (match.getName().contains(recruiter)) {
						numberOfInternalSubs++;
						
						//totalNumberOfInternalSubs++;
						intSubsString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Internal Submissions:</b> "
								+ numberOfInternalSubs + "</font>";
						//totalIntSubsString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Internal Submissions:</b> "
								//+ totalNumberOfInternalSubs + "</font>";
					
						}
					}
				}
				
				
				// find number of type for each note action
				Set<String> uniqueSet = new HashSet<String>(noteActionList);
				for (String temp : uniqueSet) {
					noteString += "\n	<br><b style='color:rgb(102,102,102);font-family:arial,sans-serif;font-size:13px'>"
							+ temp
							+ " Notes: </b>"
							+ "<font color=#666666>"
							+ Collections.frequency(noteActionList, temp) + "</font>";
				}

				text = "\n<br><blockquote style='margin: 0px 0px 0px 40px;border:none;padding:0px'><b><a href=https://cls5.bullhornstaffing.com/BullhornStaffing/OpenWindow.cfm?Entity=JobOrder&id="
						+ job
						+ " "
						+ "<span><font color=#666666>"
						+ job
						+ "</font></span>"
						+ " "
						+ titleString
						+ "</a></b>"
						+ statusString
						+ openClosedString
						+ recruiterString
						+ "\n"
						+ noteString
						+ intSubsString + clientSubsString + interviewString + placementString + "</blockquote>";
				// if it is the last job in the list then compile the jobs
				// information into totals
				compiledText += text;
				if (assocList.indexOf(job) == (assocList.size() - 1)) {
					text="";
					
				} 

				
				System.out.println(compiledText);

			}
			String queryDate = String.valueOf(startDate.getMonth()) + "/" + String.valueOf(startDate.getDay()) + "/" + String.valueOf(startDate.getYear());
			System.out.println(queryDate);
			ApiGetEntityNotesResult totNotesIds = apiService.getEntityNotesWhere(session, "CorporateUser", recruiterIdList.get(recruiterCount) , "note.dateAdded>=" + "'" + queryDate + "'");
			session = totNotesIds.getSession();
			for (Object id: totNotesIds.getIds()) {
				ApiFindResult findNote = apiService.find(session, "Note", id);
				NoteDto totNote = (NoteDto) findNote.getDto();
				if (totNote.getCommentingPersonID().equals(recruiterIdList.get(recruiterCount))) {
					totalNoteActionList.add(totNote.getAction());
				}
				
			}
			
			DtoQuery sendoutQuery = new DtoQuery();
			sendoutQuery.setEntityName("Sendout");
			sendoutQuery.setMaxResults(10000);
			sendoutQuery.setWhere( "dateAdded>=" + "'" + queryDate + "'");
			ApiQueryResult sendoutResult = apiService.query(session, sendoutQuery);
			session = sendoutResult.getSession();
			for(Object sendoutId:sendoutResult.getIds()) {
				SendoutDto sendout = (SendoutDto) apiService.find(session, "Sendout", sendoutId).getDto();
				ApiGetAssociationIdsResult associations = apiService.getAssociationIds(session, "JobOrder", sendout.getJobOrderID(), "submissions");
				if (associations.getIds().size()>=1) {
					for (Object submissionId: associations.getIds()) {
						JobSubmissionDto intSubmission = (JobSubmissionDto) apiService.find(session, "JobSubmission", submissionId).getDto();
						if (intSubmission.getCandidateID().equals(sendout.getCandidateID())) {
							if (intSubmission.getSendingUserID().equals(recruiterIdList.get(recruiterCount))) {
								totalNumberOfClientSubs++;
								totalClientSubsString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Client Submissions:</b> "
										+ totalNumberOfClientSubs + "</font>";
								}
							}
						}
					}
				}
			
			
			DtoQuery appointmentQuery = new DtoQuery();
			appointmentQuery.setEntityName("Appointment");
			appointmentQuery.setMaxResults(10000);
			appointmentQuery.setWhere("dateAdded>=" + "'" + queryDate + "'" +
					" AND " + "type='Interview'");
			ApiQueryResult interviewResult = apiService.query(session, appointmentQuery);
			session = interviewResult.getSession();
			for (Object interviewId: interviewResult.getIds()) {
				AppointmentDto interview = (AppointmentDto) apiService.find(session, "Appointment", interviewId).getDto();
				ApiGetAssociationIdsResult associations = apiService.getAssociationIds(session, "JobOrder", interview.getJobOrderID(), "submissions");
				System.out.println(interview.getJobOrderID());
				if (associations.getIds().size()>=1) {
					for (Object submissionId: associations.getIds()) {
						JobSubmissionDto intSubmission = (JobSubmissionDto) apiService.find(session, "JobSubmission", submissionId).getDto();
						if (intSubmission.getCandidateID()!=null && interview.getCandidateReferenceID()!=null) {
							if (intSubmission.getCandidateID().equals(interview.getCandidateReferenceID())) {
								if (intSubmission.getSendingUserID().equals(recruiterIdList.get(recruiterCount))) {
									totalNumberOfInterviews++;
									totalInterviewString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Interviews:</b> "
											+ totalNumberOfInterviews + "</font>";
										}
									}
								}
							}
						}
					}
				
			
			DtoQuery placementQuery = new DtoQuery();
			placementQuery.setEntityName("Placement");
			placementQuery.setMaxResults(10000);
			placementQuery.setWhere("dateAdded>=" + "'" + queryDate + "'");
			ApiQueryResult placementResult = apiService.query(session, placementQuery);
			session = placementResult.getSession();
			if (placementResult.getIds().size()>=1) {
				for (Object placementId: placementResult.getIds()) {
					PlacementDto placement = (PlacementDto) apiService.find(session, "Placement", placementId).getDto();
					if (placement.getCorrelatedCustomText2().contains(recruiter)) {
						totalNumberOfPlacements++;
						totalPlacementString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Placements:</b> "
								+ totalNumberOfPlacements + "</font>";
						}
					}
				}
			
			
			
			DtoQuery intSubmissionQuery = new DtoQuery();
			intSubmissionQuery.setEntityName("JobSubmission");
			intSubmissionQuery.setMaxResults(10000);
			intSubmissionQuery.setWhere("dateAdded>=" + "'" + queryDate + "'");
			ApiQueryResult intSubmissionResult = apiService.query(session, intSubmissionQuery);
			session = intSubmissionResult.getSession();
			if (intSubmissionResult.getIds().size()>=1) {
				for (Object intSubmissionId: intSubmissionResult.getIds()) {
					JobSubmissionDto internalSubmission = (JobSubmissionDto) apiService.find(session, "JobSubmission", intSubmissionId).getDto();
					if (internalSubmission.getSendingUserID().equals(recruiterIdList.get(recruiterCount))) {
						totalNumberOfInternalSubs++;
						totalIntSubsString = "	<br><font color=#48c3b1 face=Helvetica, Arial, sans-serif style='font-family:arial,sans-serif;font-size:13px'><b>Internal Submissions:</b> "
								+ totalNumberOfInternalSubs + "</font>";
						}
					}
				}
				
			
			Set<String>uniqueSet = new HashSet<String>(totalNoteActionList);
			for (String temp : uniqueSet) {
				totalNoteString += "\n	<br><b style='color:rgb(102,102,102);font-family:arial,sans-serif;font-size:13px'>	"
						+ temp
						+ " Notes:</b> "
						+ "<font color=#666666>"
						+ Collections.frequency(totalNoteActionList, temp) + "</font>";
			}

			text += "\n<br><blockquote style='margin: 0px 0px 0px 40px;border:none;padding:0px'> <font color=#666666 face=arial, sans-serif><u><b>All Activity:</b></u></font>"
					+ totalNoteString
					+ totalIntSubsString
					+ totalClientSubsString
					+ totalInterviewString
					+ totalPlacementString + "</blockquote>";
			
			compiledText += text;
			compiledText += "<br><br>";
			System.out.println(compiledText);
			
			recruiterCount++;
		}

		// Prep for email message
		final String username = "bullhorn@314ecorp.com";
		final String password = "Kaiser123";

		Properties props = new Properties();
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
		// send email
		try {

			Message message = new MimeMessage(mailSession);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("mimi.curiel@314ecorp.com"));
			message.setSubject("Recruiter Metrics Report for " + endDate.getMonth() + "/" + endDate.getDay() + "/"
					+ endDate.getYear());
			message.setText(compiledText);
			message.setContent(compiledText, "text/html; charset=utf-8");

			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	}

}