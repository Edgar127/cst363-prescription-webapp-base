package com.csumb.cst363;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@SuppressWarnings("unused")
@Controller    
public class ControllerPrescriptionCreate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Doctor requests blank form for new prescription.
	 * Do not modify this method.
	 */
	@GetMapping("/prescription/new")
	public String getPrescriptionForm(Model model) {
		model.addAttribute("prescription", new Prescription());
		return "prescription_create";
	}
	

	/*
	 * Doctor creates a prescription.
	 */
	@PostMapping("/prescription")
	public String createPrescription(Prescription p, Model model) {

		System.out.println("createPrescription " + p);

		// TODO

		/*-
		 * Process the new prescription form.
		 * 1. Obtain connection to database. 
		 * 2. Validate that doctor id and name exists 
		 * 3. Validate that patient id and name exists 
		 * 4. Validate that Drug name exists and obtain drug id.
		 * 5. Insert new prescription 
		 * 6. Get generated value for rxid 
		 * 7. Update prescription object and return
		 */

		//	1. Obtain connection to database.
		try (Connection con = getConnection();) {

			PreparedStatement psSelectPharmacy = con.prepareStatement("SELECT * FROM Pharmacy ORDER BY RAND() LIMIT 1");
			ResultSet randomPharmacy = psSelectPharmacy.executeQuery();

			if(!randomPharmacy.next()){
				throw new SQLException("No pharmacies exist");
			}
			int randomPharmacyID = randomPharmacy.getInt(1);
			p.setPharmacyID(randomPharmacyID);

			//	2. Validate that doctor id and name exists
			PreparedStatement psDoctor = con.prepareStatement("SELECT * FROM Doctor WHERE ID = ?");
			psDoctor.setInt(1, p.getDoctor_id());

			ResultSet rsDoctor = psDoctor.executeQuery();

			if(!rsDoctor.next()) {
				throw new SQLException("Doctor with ID " + p.getDoctor_id() + " doesn't exist.");
			}

			String doctorLastName = rsDoctor.getString(3);

			if(!Objects.equals(doctorLastName, p.getDoctorLastName())) {
				throw new SQLException("Doctor with Last Name " + p.getDoctorLastName() + " doesn't exist.");
			}

			//	3. Validate that patient id and name exists
			PreparedStatement psPatient = con.prepareStatement("SELECT * FROM Patient WHERE ID = ?");
			psPatient.setInt(1, p.getPatient_id());

			ResultSet rsPatient = psPatient.executeQuery();

			if(!rsPatient.next()) {
				throw new SQLException("Patient with ID " + p.getPatient_id() + " doesn't exist.");
			}

			String patientLastName = rsPatient.getString(3);

			if(!Objects.equals(patientLastName, p.getPatientLastName())) {
				throw new SQLException("Patient with Last Name " + p.getPatientLastName() + " doesn't exist.");
			}

			//	4. Validate that Drug name exists and obtain drug id.
			PreparedStatement psDrug = con.prepareStatement("SELECT * FROM Drug WHERE Name = ?");
			psDrug.setString(1, p.getDrugName());

			ResultSet rsDrug = psDrug.executeQuery();

			if(!rsDrug.next()) {
				throw new SQLException("Drug with Name " + p.getDrugName() + " doesn't exist.");
			}

			//	5. Insert new prescription
			String psInsertPresQuery = "INSERT INTO Prescription (Doctor_id, Patient_id, Drug_name, Date_prescribed, Quantity) VALUES(?, ?, ?, ?, ?)";
			PreparedStatement psInsertPrescription = con.prepareStatement(psInsertPresQuery, Statement.RETURN_GENERATED_KEYS);
			psInsertPrescription.setInt(1, p.getDoctor_id());
			psInsertPrescription.setInt(2, p.getPatient_id());
			psInsertPrescription.setString(3, p.getDrugName());
			psInsertPrescription.setString(4, p.getDateCreated());
			psInsertPrescription.setInt(5, p.getQuantity());

			psInsertPrescription.executeUpdate();

			String retrieveHighestRXIDQuery = "SELECT MAX(Prescription_RXID) FROM PrescriptionRefill";
			PreparedStatement retrieveHighestRXIDStatement = con.prepareStatement(retrieveHighestRXIDQuery);
			ResultSet retrieveHighestRXIDResult = retrieveHighestRXIDStatement.executeQuery();
			retrieveHighestRXIDResult.next();

			int rxid = retrieveHighestRXIDResult.getInt(1) + 1;
			String psInsertPresRefillQuery = "INSERT INTO PrescriptionRefill (Prescription_RXID, Refill_count, Pharmacy_ID) VALUES(?, ?, ?)";
			PreparedStatement psInsertPrescriptionRefill = con.prepareStatement(psInsertPresRefillQuery);
			psInsertPrescriptionRefill.setInt(1, rxid);
			psInsertPrescriptionRefill.setInt(2, 1);
			psInsertPrescriptionRefill.setInt(3, randomPharmacyID);

			psInsertPrescriptionRefill.executeUpdate();
			p.setRxid(String.valueOf(rxid));
			p.setRefills(1);

			// 7. Update prescription object and return
			model.addAttribute("message", "Prescription created.");
			model.addAttribute("prescription", p);
			return "prescription_show";
		} catch (SQLException  e) {
			// if there is error
			// model.addAttribute("message",  <error message>);
			// model.addAttribute("prescription", p);
			// return "prescription_create";

			model.addAttribute("message", "SQL Error: " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		} catch (Exception e) {
			model.addAttribute("message", "Error: " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}
	}
	
	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
	
}
