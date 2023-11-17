package com.csumb.cst363;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

			//	2. Validate that doctor id and name exists
			PreparedStatement psDoctor = con.prepareStatement("SELECT * FROM Doctor WHERE ID = ?",
					Statement.RETURN_GENERATED_KEYS);
			psDoctor.setInt(1, p.getDoctor_id());

			ResultSet rsDoctor = psDoctor.executeQuery();

			// TODO: I'm not sure if we actually need to verify that the name exists, I'd say we do but I'll leave it up to you guys
			if(!rsDoctor.next()) {
				throw new SQLException("Doctor with ID " + p.getDoctor_id() + " doesn't exist.");
			}

			//	3. Validate that patient id and name exists
			PreparedStatement psPatient = con.prepareStatement("SELECT * FROM Patient WHERE ID = ?",
					Statement.RETURN_GENERATED_KEYS);
			psPatient.setInt(1, p.getPatient_id());

			ResultSet rsPatient = psPatient.executeQuery();

			// TODO: I'm not sure if we actually need to verify that the name exists, I'd say we do but I'll leave it up to you guys
			if(!rsPatient.next()) {
				throw new SQLException("Patient with ID " + p.getPatient_id() + " doesn't exist.");
			}

			//	4. Validate that Drug name exists and obtain drug id.
			PreparedStatement psDrug = con.prepareStatement("SELECT * FROM Drug WHERE Name = ?",
					Statement.RETURN_GENERATED_KEYS);
			psDrug.setString(1, p.getDrugName());

			ResultSet rsDrug = psDrug.executeQuery();

			// TODO: I'm not sure if we actually need to verify that the drug name exists, I'd say we do but I'll leave it up to you guys
			if(!rsDrug.next()) {
				throw new SQLException("Drug with Name " + p.getDrugName() + " doesn't exist.");
			}

			//	5. Insert new prescription
			PreparedStatement psInsertPrescription = con.prepareStatement(
					"INSERT INTO Prescription" +
							"(Doctor_id, Patient_id, Drug_name, Date_prescribed, Quantity_refills)" +
							"VALUES(?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			psInsertPrescription.setInt(1, p.getDoctor_id());
			psInsertPrescription.setInt(2, p.getPatient_id());
			psInsertPrescription.setString(3, p.getDrugName());
			psInsertPrescription.setString(4, p.getDateCreated());
			psInsertPrescription.setInt(5, p.getQuantity());

			psInsertPrescription.executeUpdate();

			ResultSet rsInsertPrescription = psInsertPrescription.getGeneratedKeys();

			//	6. Get generated value for rxid
			// TODO: Okay, we get the value but what will it be used for? It can't be to set the RXID because the DB is generating the RXID on the insert
//			int rxid;
//			if(rsInsertPrescription.next()) {
//				rxid = rsInsertPrescription.getInt(1);
//			}


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
			return "prescription_register";
		} catch (Exception e) {
			model.addAttribute("message", "Error: " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_register";
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
