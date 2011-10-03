package org.openmrs.module.patientdashboard.web.controller;

import java.util.HashSet;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.EncounterType;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.hospitalcore.IpdService;
import org.openmrs.module.hospitalcore.model.IpdPatientAdmitted;
import org.openmrs.module.hospitalcore.util.PatientDashboardConstants;
import org.openmrs.module.hospitalcore.util.PatientUtil;
import org.openmrs.module.patientdashboard.util.PatientDashboardUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller("patientDashboardMainController")
@RequestMapping("/module/patientdashboard/main.htm")
public class MainController {

	@RequestMapping(method = RequestMethod.GET)
	public String firstView(@RequestParam("patientId") Integer patientId,
			@RequestParam("opdId") Integer opdId,
			@RequestParam(value="queueId", required=false) Integer queueId,
			@RequestParam("referralId") Integer referralId, Model model) {
		PatientService ps = Context.getPatientService();
		Patient patient = ps.getPatient(patientId);
		model.addAttribute("patient", patient);
		model.addAttribute("patientCategory",PatientUtil.getPatientCategory(patient));
		model.addAttribute("queueId",queueId);
		model.addAttribute("age", PatientDashboardUtil.getAgeFromBirthDate(
				patient.getBirthdate(), patient.getBirthdateEstimated()));
		model.addAttribute("ageCategory",
				PatientDashboardUtil.calcAgeClass(patient.getAge()));
		model.addAttribute("opd", Context.getConceptService().getConcept(opdId));
		model.addAttribute("referral",
				Context.getConceptService().getConcept(referralId));
		

		

		/*
		 * String isInit = Context.getAdministrationService().getGlobalProperty(
		 * PatientDashboardConstants.PROPERTY_INIT_CONCEPT); if(
		 * "false".equals(isInit)){ ConceptService conceptService =
		 * Context.getConceptService();
		 * 
		 * ConceptDatatype datatype =
		 * Context.getConceptService().getConceptDatatypeByName("N/A");
		 * ConceptClass conceptClass =
		 * conceptService.getConceptClassByName("Misc"); String conceptName =
		 * Context
		 * .getAdministrationService().getGlobalProperty(PatientDashboardConstants
		 * .PROPERTY_PROVISIONAL_DIAGNOSIS); Concept con =
		 * conceptService.getConcept(conceptName); if( con == null ){ con = new
		 * Concept(); ConceptName name = new ConceptName(conceptName,
		 * Context.getLocale()); con.addName(name); con.setDatatype(datatype);
		 * con.setConceptClass(conceptClass); conceptService.saveConcept(con); }
		 * }
		 */


		// Init needed concepts
		
		insertPropertiesUnlessExist();
		
		// get admitted status of patient
		
		IpdService ipdService = Context.getService(IpdService.class);
		
		IpdPatientAdmitted admitted = ipdService.getAdmittedByPatientId(patientId);
		
		if( admitted != null ){
			model.addAttribute("admittedStatus", "Admitted");
		}
		
		return "module/patientdashboard/main";
	}

	private void insertPropertiesUnlessExist() {

		GlobalProperty isInit = getGlobalProperty();

		if ("0".equals(isInit.getPropertyValue())) {

			// System.out.println("run it");

			try {
				isInit.setPropertyValue("1");
				Context.getAdministrationService().saveGlobalProperty(isInit);

				ConceptService conceptService = Context.getConceptService();

				// external hospital
				insertConcept(conceptService, "Coded", "Question",
						PatientDashboardConstants.PROPERTY_HOSPITAL);

				// Provisional diagnosis
				insertConcept(
						conceptService,
						"N/A",
						"Misc",
						PatientDashboardConstants.PROPERTY_PROVISIONAL_DIAGNOSIS);

				// Post for procedure
				insertConcept(conceptService, "N/A", "Misc",
						PatientDashboardConstants.PROPERTY_POST_FOR_PROCEDURE);

				// Internal referral
				insertConcept(conceptService, "Coded", "Question",
						PatientDashboardConstants.PROPERTY_INTERNAL_REFERRAL);

				// External referral
				insertConcept(conceptService, "Coded", "Question",
						PatientDashboardConstants.PROPERTY_EXTERNAL_REFERRAL);

				// Visit outcome
				insertConcept(conceptService, "Text", "Misc",
						PatientDashboardConstants.PROPERTY_VISIT_OUTCOME);

				// OPD WARD
				insertConcept(conceptService, "Coded", "Question",
						PatientDashboardConstants.PROPERTY_OPDWARD);

				// IPD WARD
				insertConcept(conceptService, "Coded", "Question",
						PatientDashboardConstants.PROPERTY_IPDWARD);

				// OPD encounter
				insertEncounter(PatientDashboardConstants.PROPERTY_OPD_ENCOUTNER_TYPE);

				// LAB encounter
				insertEncounter(PatientDashboardConstants.PROPERTY_LAB_ENCOUTNER_TYPE);

				/*
				 * Add external hospitals CHANGE LATER
				 */
				// insertExternalHospitalConcepts(conceptService);
				// insertIpdWardConcepts(conceptService);
				// Change the global property to 2
				isInit.setPropertyValue("2");
				Context.getAdministrationService().saveGlobalProperty(isInit);

			} catch (Exception e) {
				e.printStackTrace();
				isInit.setPropertyValue("0");
				Context.getAdministrationService().saveGlobalProperty(isInit);
			}

		}
	}
	
	// Return the globalProperty to tell necessary concepts are created.
	// If it does not exist, create the new one with value 0
	private GlobalProperty getGlobalProperty(){

		GlobalProperty gp = Context.getAdministrationService()
				.getGlobalPropertyObject(
						PatientDashboardConstants.PROPERTY_INIT_CONCEPT);

		if (gp == null) {
			gp = new GlobalProperty(
					PatientDashboardConstants.PROPERTY_INIT_CONCEPT, "0");
		}

		try {
			Integer.parseInt(gp.getPropertyValue());
		} catch (Exception e) {
			gp.setPropertyValue("0");
		}
		
		return gp;
	}

	private Concept insertConcept(ConceptService conceptService,
			String dataTypeName, String conceptClassName, String conceptNameKey) {
		try {
			ConceptDatatype datatype = Context.getConceptService()
					.getConceptDatatypeByName(dataTypeName);
			ConceptClass conceptClass = conceptService
					.getConceptClassByName(conceptClassName);
			GlobalProperty gp = Context.getAdministrationService()
					.getGlobalPropertyObject(conceptNameKey);
			Concept con = conceptService.getConcept(gp.getPropertyValue());
			// System.out.println(con);
			if (con == null) {
				con = new Concept();
				ConceptName name = new ConceptName(gp.getPropertyValue(),
						Context.getLocale());
				con.addName(name);
				con.setDatatype(datatype);
				con.setConceptClass(conceptClass);
				return conceptService.saveConcept(con);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private Concept insertHospital(ConceptService conceptService,
			String hospitalName) {
		try {
			ConceptDatatype datatype = Context.getConceptService()
					.getConceptDatatypeByName("N/A");
			ConceptClass conceptClass = conceptService
					.getConceptClassByName("Misc");
			Concept con = conceptService.getConceptByName(hospitalName);
			// System.out.println(con);
			if (con == null) {
				con = new Concept();
				ConceptName name = new ConceptName(hospitalName,
						Context.getLocale());
				con.addName(name);
				con.setDatatype(datatype);
				con.setConceptClass(conceptClass);
				return conceptService.saveConcept(con);
			}
			return con;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void insertEncounter(String typeKey) {
		try {
			GlobalProperty gp = Context.getAdministrationService()
					.getGlobalPropertyObject(typeKey);
			if (Context.getEncounterService().getEncounterType(
					gp.getPropertyValue()) == null) {
				EncounterType et = new EncounterType(gp.getPropertyValue(), "");
				Context.getEncounterService().saveEncounterType(et);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void insertExternalHospitalConcepts(ConceptService conceptService) {
		// System.out.println("========= insertExternalHospitalConcepts =========");
		Concept concept = conceptService
				.getConcept(PatientDashboardConstants.PROPERTY_HOSPITAL);
		if (concept != null) {
			String[] hospitalNames = { "INDIRA GANDHI MEDICAL COLLLEGE",
					"POST GRADUATE INSTITUTE, CHANDIGARH",
					"ALL INDIA INSTITUTE OF MEDICAL SCIENCE, NEW DELHI"
					};
			for (String hn : hospitalNames) {
				insertHospital(conceptService, hn);
			}
			addConceptAnswers(concept, hospitalNames,
					Context.getAuthenticatedUser());
		}
	}
	
	private void insertIpdWardConcepts(ConceptService conceptService) {
		// System.out.println("========= insertExternalHospitalConcepts =========");
		Concept concept = conceptService.getConcept(PatientDashboardConstants.PROPERTY_IPDWARD);
		if (concept != null) {
			String[] wards = { "Ipd Ward 1",
					"Ipd Ward 2",
					"Ipd Ward 3" };
			for (String hn : wards) {
				insertHospital(conceptService, hn);
			}
			addConceptAnswers(concept, wards,
					Context.getAuthenticatedUser());
		}
	}

	private void addConceptAnswers(Concept concept, String[] answerNames,
			User creator) {
		Set<Integer> currentAnswerIds = new HashSet<Integer>();
		for (ConceptAnswer answer : concept.getAnswers()) {
			currentAnswerIds.add(answer.getAnswerConcept().getConceptId());
		}
		boolean changed = false;
		for (String answerName : answerNames) {
			Concept answer = Context.getConceptService().getConcept(answerName);
			if (!currentAnswerIds.contains(answer.getConceptId())) {
				changed = true;
				ConceptAnswer conceptAnswer = new ConceptAnswer(answer);
				conceptAnswer.setCreator(creator);
				concept.addAnswer(conceptAnswer);
			}
		}
		if (changed) {
			Context.getConceptService().saveConcept(concept);
		}
	}

}