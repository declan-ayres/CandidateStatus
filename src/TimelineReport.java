import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

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
import com.bullhorn.apiservice.result.ApiQueryResult;
import com.bullhorn.apiservice.result.ApiStartSessionResult;
import com.bullhorn.entity.job.JobOrderDto;
import com.bullhorn.entity.user.CorporateUserDto;





class TimelineReport {
	
	
	private static final QName SERVICE_NAME = new QName("http://apiservice.bullhorn.com/", "ApiService");
	private static final String WSDL_URL = "https://api.bullhornstaffing.com/webservices-2.5/?wsdl";
	private static final String API_USER = "314e.API";
	private static final String API_PASSWD = "314e.com";
	private static final String API_KEY = "F5EA5835-9A07-81A4-448468FE85A30556";
	static Properties properties = new Properties();
	

	public static void main(String[] args) throws Exception {
		
		properties.load(TimelineReport.class.getResourceAsStream("/bullhorn.properties"));
		
		final URL serviceUrl = new URL(ApiService_Service.class.getResource("."), WSDL_URL);
		
		final ApiService apiService = new ApiService_Service(serviceUrl, SERVICE_NAME).getApiServicePort();
		
		final ApiStartSessionResult startSession = apiService.startSession(API_USER, API_PASSWD, API_KEY);
		
		
		String session = startSession.getSession();	
		

		//get all recruiters
		DtoQuery distQuery = new DtoQuery();
		distQuery.setEntityName("DistributionList");
		distQuery.setMaxResults(100);
		distQuery.setWhere("name=" + "'" + properties.getProperty("distributionList") + "'");
		
		ApiQueryResult distResults = apiService.query(session, distQuery);
		
		
		ApiGetAssociationIdsResult recruiters =  apiService.getAssociationIds(session, "DistributionList", distResults.getIds().get(0), "members");
		
		
		ArrayList<String> recruiterList = new ArrayList<>();
		ArrayList<Integer> recruiterIdList = new ArrayList<>();
		
		//sort and get active recruiters
		for (int index=0; index<recruiters.getIds().size(); index++) {
			ApiFindResult findRecruiter = apiService.find(session, "CorporateUser", recruiters.getIds().get(index));
			CorporateUserDto recruiter = (CorporateUserDto) findRecruiter.getDto();
			if (recruiter.isIsDeleted().equals(true) || recruiter.getFirstName().equals("Abhishek") || recruiter.getFirstName().equals("Alok")) {
				
			} else {
				recruiterList.add(recruiter.getName());
				recruiterIdList.add(recruiter.getUserID());
			}
				
			
		}
		
		Date currDate = new Date();
		
		DateFormat format = new SimpleDateFormat("MM/dd/yyyy");
		Date tempDate = format.parse((String) properties.getOrDefault("startDate", currDate));
		
		
		GregorianCalendar c1 = new GregorianCalendar();
		c1.setTime(tempDate);
		XMLGregorianCalendar endDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c1);
		System.out.println(endDate);
		
		String endDateString = endDate.getMonth() + "/" + endDate.getDay() + "/" + endDate.getYear();

		tempDate = format.parse((String) properties.getOrDefault("endDate", currDate));
		c1.setTime(tempDate);
		XMLGregorianCalendar startDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(c1);
		System.out.println(startDate);
		
		String startDateString = startDate.getMonth() + "/" + startDate.getDay() +"/" + startDate.getYear();
		
	
		c1.setTime(currDate);
		XMLGregorianCalendar startTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(c1);
		startTime.setHour(Integer.parseInt(((properties.getProperty("startTime")).split(":"))[0]));
		startTime.setMinute(Integer.parseInt(((properties.getProperty("startTime")).split(":"))[1]));
		System.out.println(startTime);
		
		
		XMLGregorianCalendar endTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(c1);
		endTime.setHour(Integer.parseInt(((properties.getProperty("endTime")).split(":"))[0]));
		endTime.setMinute(Integer.parseInt(((properties.getProperty("endTime")).split(":"))[1]));
		System.out.println(endTime);
		
		

		//get jobs in the past 6 months
		DtoQuery jobQuery = new DtoQuery();
		jobQuery.setEntityName("JobOrder");
		jobQuery.setMaxResults(100000);
		jobQuery.setWhere("dateAdded<" + "'" + endDateString + "'" + " AND " + "dateAdded>'1/1/2014'" );
		jobQuery.getOrderBys().add("dateAdded desc");
		ApiQueryResult jobResults = apiService.query(session, jobQuery);
		
		
		
		
		List<List<Object>> jobsList = new ArrayList<List< Object>>();
		List<Object> jobAndRecruiter = new ArrayList<Object>();
		int count=0;
		
		
	
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
							if (fieldChange.getColumnName().equals("correlatedCustomText2")) {
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
						} else if (fieldChange.getColumnName().equals("CorrelatedCustomText2")) {
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
					
					for (List<Object> pair: jobsList) {
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

						if (!(pair.get(0).equals(""))) {
							
							
						}
					}
				}

		 
		 
		 //prepare dates for start and end dates for edit history
		
		
		
	
	}
	
	
}