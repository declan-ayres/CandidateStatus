import java.net.URL;








import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;










import com.bullhorn.apiservice.ApiService;
import com.bullhorn.apiservice.ApiService_Service;
import com.bullhorn.apiservice.query.DtoQuery;
import com.bullhorn.apiservice.result.ApiFindResult;
import com.bullhorn.apiservice.result.ApiFindMultipleResult;
import com.bullhorn.apiservice.result.ApiGetNoteReferencesResult;
import com.bullhorn.apiservice.result.ApiQueryResult;
import com.bullhorn.apiservice.result.ApiStartSessionResult;
import com.bullhorn.entity.ApiEntityName;
import com.bullhorn.entity.note.*;
import com.bullhorn.apiservice.result.*;
import com.bullhorn.entity.candidate.*;
import com.bullhorn.entity.template.user.*;
import com.bullhorn.entity.template.client.*;
import com.bullhorn.entity.template.job.*;
import com.bullhorn.entity.lists.*;
import com.bullhorn.apiservice.result.ApiGetEntityMetadataResult;
import com.bullhorn.apiservice.struct.EntityNameIdPair;
import com.bullhorn.apiservice.meta.*;
import com.bullhorn.apiservice.edithistory.*;

import java.io.UnsupportedEncodingException;

import javax.mail.PasswordAuthentication;

import com.sun.mail.smtp.SMTPTransport;

import javax.mail.Transport;

import java.security.Security;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;




class Bullhorn{
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
	
	
	
	
	DtoQuery query2 = new DtoQuery();
	query2.setEntityName("Candidate");
	query2.setMaxResults(30000);
	query2.getOrderBys().add("id asc");
	query2.setWhere(/*"name='(TEST) Jack Sparrow'" + " " + "OR" + " " + "name='(TEST) James Bond'" +
	" " + "OR" + " " + "name='(TEST) Forrest Gump'" + " " + "OR" + " " + "name='(TEST) Walter White'" +
			" " + "OR" + " " + "name='(TEST) Bruce Wayne'" + " " + "OR" + " " + "name='(TEST) Mary Poppins'" + 
	" " + "OR" + " " + "name='(TEST) Luke Skywalker'" + " " + "OR" + " " + "name='(TEST) Hermione Granger'" +
			" " + "OR" + " " + "name='(TEST) Miss Piggy'"*/ "isDeleted=0" + " " + "AND NOT" + " " + "status = 'archive'");
	
	
	ApiQueryResult qResult2=apiService.query(session, query2);
	ApiFindResult candidateResults= apiService.find(session,
			"Candidate", qResult2.getIds().get(0));
	
	//System.out.println(qResult2.getIds());
	//System.out.println(qResult2.getIds().size());
	CandidateDto candidate=(CandidateDto) candidateResults.getDto();
	//System.out.println(qResult2.getIds().get(7));
	
	
	
	
	/*ApiGetEntityNotesResult Associated_note= apiService.getEntityNotes(session, "Candidate", (int) qResult2.getIds().get(7));
	ApiFindResult findNoteResult = apiService.find(session, "Note", Associated_note.getIds().get(2));
	
	System.out.println(Associated_note.getIds());
	NoteDto test_note = (NoteDto) findNoteResult.getDto();
	*/
	
	
	
	
	 final String username = "declan.ayres@314ecorp.com";
     final String password = "declan123";

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

     /*try {

         Message message = new MimeMessage(mailSession);
         message.setFrom(new InternetAddress(username));
         message.setRecipients(Message.RecipientType.TO,
             InternetAddress.parse("declan.ayres@314ecorp.com"));
         message.setSubject("Test");
         message.setText("This is a test.");

         Transport.send(message);

         System.out.println("Done");

     } catch (MessagingException e) {
         throw new RuntimeException(e);
     }*/
	
	
	String compiledChanges = new String();
	
	compiledChanges = "";
	
	System.out.println(compiledChanges);
	
	String temp = "";
	
	compiledChanges+="\n\n" + temp;
	
	System.out.println(compiledChanges);
	
	
	XMLGregorianCalendar da;
	
	 
	
	org.joda.time.format.DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE MMMM dd, yyyy");
	DateTime date; 
	
	int day;
	int month;
	int year;
	int hour;
	int minute;
	
	search:
	for (int index = 0; index< qResult2.getIds().size(); index++) {
	
		candidateResults= apiService.find(session,
				"Candidate", qResult2.getIds().get(index));
		session = candidateResults.getSession();
		candidate=(CandidateDto) candidateResults.getDto();
		ApiGetEditHistoryResult history = apiService.getEditHistory(session, "Candidate", qResult2.getIds().get(index));
		List<ApiEditHistory> histObjects = history.getEditHistories();
		session = history.getSession();
	
		for (int i = 0; i< histObjects.size(); i++) {
	
				List<ApiEditHistoryFieldChange> histChanges = histObjects.get(i).getFieldChanges();
				
				for (ApiEditHistoryFieldChange item: histChanges) {
					
					if (item.getColumnName().equals("status")) {
						
						da = histObjects.get(i).getDateAdded();
					
						day = da.getDay();
						month = da.getMonth();
						year = da.getYear();
						hour = da.getHour();
						minute = da.getMinute();
						
						
						date = new DateTime(year, month, day, hour, minute, 0);
						
						
						
				
						temp = "<ul>" + "<li style=font-size:14px;>" + "<span style='color:#009900;font-family:Century Gothic;'>" + candidate.getName() + "'s" + "</span>" + 
						" " + "<span style=font-size:15px;>" + "<b>" + item.getColumnName() + "</b>" + "</span>" +  " " + "was changed from" + " " +
						"'" + "<i>" + "<strong>" + item.getOldValue() + "</strong>" + "</i>" +  "'" +  " " + "to" + " " +
								"'" + "<i>" + "<strong>" + item.getNewValue() + "</strong>" + "</i>" + "'" + " " + "on the date of:" + " " + "<b>" + date.minusHours(3).toString(formatter) + "</b>" + "</li>" + "</ul>";            
						
						
						compiledChanges+="\n\n" + "<br>" + temp + "<br>";
						
						System.out.println(candidate.getName() + " " + (index+1));
						System.out.println(temp);
						System.out.println(compiledChanges);
						
						//System.out.println(item.getColumnName() + " " + "was changed from" + " " + "'" + item.getOldValue() + "'" +  " " + "to" + " " +
						//"'" + item.getNewValue() + "'" + " " + "on the date of:" + " " + histObjects.get(i).getDateAdded() );
						
						
						
						
						System.out.println("\n\n\n\n\n");
						
						
						if (index>10) {
							break search;
						}
						
					}
					
				}
	
			}
		
		}
	
	
	try {

			Message message = new MimeMessage(mailSession);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("mimi.curiel@314ecorp.com"));
			message.setSubject("Status changes");
			message.setText(compiledChanges);
			message.setContent("Hi Mimi,<br>I formatted the status changes for maximum readability. Here's an example of what it looks like. Let me know"
					+ " if you want any other formatting changes."
					+ "Also, I can change the status to candidate rating but I dont know what the custom tab for that is.<br><br>Thanks,<br>Declan" + compiledChanges, "text/html; charset=utf-8");

			Transport.send(message);

			System.out.println("Done");

			} catch (MessagingException e) {
			throw new RuntimeException(e);
		}

	
	
	
	
	

	
	/*
	System.out.println(" ");
	
	for (int index=0; index < candidateResults.getDtos().size(); index++) {
		System.out.println(" ");
		
		candidate= (CandidateDto) candidateResults.getDtos().get(index);
		System.out.println(qResult2.getIds().get(index));
		System.out.println(candidate.getName());
		
		if (candidate.getDateLastComment()!=null) {
			
			Associated_note = apiService.getEntityNotes(session, "Candidate", (int) qResult2.getIds().get(index));
			session = Associated_note.getSession();
			System.out.println("Most recent note:" + " " + candidate.getDateLastComment());
			candidate.setCustomDate2(null);
			System.out.println("CustomDate2:" + " " + candidate.getCustomDate2());
			
			
			if (Associated_note.getIds().size()>1){
				for (int index1=Associated_note.getIds().size()-1; index1>=0; index1--) {
		    		findNoteResult = apiService.find(session, "Note", Associated_note.getIds().get(index1));
		    		test_note = (NoteDto) findNoteResult.getDto();
		    		
		    		
		    	if (test_note.getAction().equals("Inbound Call") ||
		    			test_note.getAction().equals("Outbound Call") ||
		    					test_note.getAction().equals("Prescreened") ||
		    							test_note.getAction().equals("Interview")) {
		    		
		    		XMLGregorianCalendar noteDate = test_note.getDateAdded();
		    		System.out.println(test_note.getAction());
		    		System.out.println(noteDate);
		    		break;
		    		}
				}
				
				
			}else{
			
				findNoteResult=apiService.find(session, "Note", Associated_note.getIds().get(0));
				session = findNoteResult.getSession();
				test_note = (NoteDto) findNoteResult.getDto();
				if (test_note.getAction().equals("Inbound Call") ||
		    			test_note.getAction().equals("Outbound Call") ||
							test_note.getAction().equals("Prescreened") ||
								test_note.getAction().equals("Interview")) {
		    		
		    		System.out.println(test_note.getAction());
		    	}else{
		    		System.out.println(test_note.getAction());
		    		}
			}
		
		
		}else{
			
			System.out.println(candidate.getDateLastComment());
		}
		
	}
     
     
	
     //Get the last week from current date
     
     DateTime dt = new DateTime();
     
     DateTime lwd = dt.minusDays(9);
     
     String lastWeek = lwd.toLocalDate().toString("MM/dd/yyyy");
    
     
     //Get all the notes from last week
    DtoQuery query = new DtoQuery();
 	query.setEntityName("Note");
 	query.setMaxResults(750000000);
 	query.setWhere("dateAdded>=" + "'" + lastWeek + "'" +  " " + "AND" + " " + "isDeleted=0" + " " + "AND" + " " + "(action='Outbound Call'" + " " + "OR" + " " + "action='Inbound Call'" + 
 			" " + "OR" + " " + "action='Prescreened'" + " " + "OR" + " " + "action='Interview')" );	
 	query.getOrderBys().add("dateAdded asc");
 	
 	
 	
 	
 	ApiQueryResult qResult= apiService.query(session, query);
 	session =qResult.getSession();
 	System.out.println(qResult.getIds());
 	System.out.println(qResult.getIds().size());
 	ApiFindResult noteResults= apiService.find(session, "Note", qResult.getIds().get(0));
 	
 	int noteId = (int) qResult.getIds().get(0);
 	
 	NoteDto weeklyNote = (NoteDto) noteResults.getDto();
 	
 	
 	DtoQuery query1 = new DtoQuery();
 	query1.setEntityName("Candidate");
 	query1.setMaxResults(1);
 	query1.setWhere("userID='52452'");
 	
 	ApiQueryResult candConf = apiService.query(session, query1);
 	
 	
 	ApiSaveResult updated;
 	
 	
	//Run through the notes
	for (int index = 0; index< qResult.getIds().size(); index++) {
		
		noteId = (int) qResult.getIds().get(index);
		
		noteResults = apiService.find(session, "Note", noteId);
		session = noteResults.getSession();
		
		ApiGetNoteReferencesResult reference = apiService.getNoteReferences(session, noteId);
		session = reference.getSession();
		
		weeklyNote = (NoteDto) noteResults.getDto();
		
		
		
		System.out.println(noteId + " " + (index+1));
		System.out.println(weeklyNote.getDateAdded());
		
		
		//Go through the references of each note
		for (EntityNameIdPair pair: reference.getEntityList()) {
		
			
			
			//Perform query search to see if person id is candidate or not.
			query1 = new DtoQuery();
			query1.setEntityName("Candidate");
			query1.setMaxResults(1);
			query1.setWhere("userID=" + pair.getEntityId().toString());
			
			candConf = apiService.query(session, query1);
			session = candConf.getSession();
			
			if (candConf.getIds().size() == 1 && pair.getEntityName().equals("Person")) {
				
				System.out.println(pair.getEntityId().toString() + " " +
						pair.getEntityName().toString());
				
				candidateResults = apiService.find(session, "Candidate", candConf.getIds().get(0));
				session = candidateResults.getSession();
				
				candidate = (CandidateDto) candidateResults.getDto();
				
				candidate.setCustomDate2(weeklyNote.getDateAdded());
				System.out.println(candidate.getCustomDate2());
				
				updated = apiService.save(session, candidate);
				session = updated.getSession();
				
				
			}
			
		
		}
		
		System.out.println(" ");
	}*/
 	
	
 }


}



