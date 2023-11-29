package com.csumb.cst363;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
@SuppressWarnings("unused")
@Controller
public class ControllerPrescriptionFill {
	@Autowired
	private JdbcTemplate jdbcTemplate;
	/*
	 * Patient requests form to search for prescription.
	 * Do not modify this method.
	 */
	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new Prescription());
		return "prescription_fill";
	}
	/*
	 * Pharmacy fills prescription.
	 */
	@PostMapping("/prescription/fill")
	public String processFillForm(Prescription p, Model model) {
		System.out.println("processFillForm " + p);

		try (Connection con = getConnection();) {
			// ----------------- verify pharmacy exists -----------------
			String pharmacyQuery = "SELECT * FROM pharmacy WHERE name = ? AND address = ?";
			PreparedStatement pharmacyStatement = con.prepareStatement(pharmacyQuery);
			pharmacyStatement.setString(1, p.getPharmacyName());
			pharmacyStatement.setString(2, p.getPharmacyAddress());

			ResultSet pharmacyResult = pharmacyStatement.executeQuery();

			if(!pharmacyResult.next()) {
				throw new SQLException("Pharmacy with Name " + p.getPharmacyName() + " and Address " + p.getPharmacyAddress() + " doesn't exists!");
			}

			// Setting remaining pharmacy information
			p.setPharmacyID(pharmacyResult.getInt("ID"));
			p.setPharmacyPhone(pharmacyResult.getString("Phone"));

			// ----------------- verify patient exists -----------------
			String searchingPatientQuery = "SELECT * FROM Patient WHERE Last_name = ?";
			PreparedStatement searchingPatientStatement = con.prepareStatement(searchingPatientQuery);
			searchingPatientStatement.setString(1, p.getPatientLastName());

			ResultSet searchingPatientResult = searchingPatientStatement.executeQuery();

			if(!searchingPatientResult.next()) {
				throw new SQLException("Patient with Last name " + p.getPatientLastName() + " doesn't exists");
			}

			// Setting remaining patient information
			p.setPatient_id(searchingPatientResult.getInt("ID"));
			p.setPatientFirstName(searchingPatientResult.getString("First_name"));

			// Setting Patient's Primary Doctor information
			String retrieveDoctorInformationQuery = "SELECT * FROM Doctor WHERE Last_name = ?";
			PreparedStatement retrieveDoctorInformationStatement = con.prepareStatement(retrieveDoctorInformationQuery);
			retrieveDoctorInformationStatement.setString(1, searchingPatientResult.getString("Primary_name"));

			ResultSet retrieveDoctorInformationResult = retrieveDoctorInformationStatement.executeQuery();
			retrieveDoctorInformationResult.next();

			p.setDoctor_id(retrieveDoctorInformationResult.getInt("ID"));
			p.setDoctorFirstName(retrieveDoctorInformationResult.getString("First_name"));
			p.setDoctorLastName(retrieveDoctorInformationResult.getString("Last_name"));

			// ----------------- verify prescription exists -----------------
			String prescriptionQuery = "SELECT * FROM prescription WHERE rxid = ? AND patient_id = ?";
			PreparedStatement prescriptionStatement = con.prepareStatement(prescriptionQuery);
			prescriptionStatement.setInt(1, Integer.parseInt(p.getRxid()));
			prescriptionStatement.setInt(2, searchingPatientResult.getInt("ID"));

			ResultSet prescriptionResult = prescriptionStatement.executeQuery();

			if(!prescriptionResult.next()) {
				throw new SQLException("Prescription with RXID " + p.getRxid() + " doesn't exists");
			}

			// Setting Patient's Primary Doctor information
			p.setDrugName(prescriptionResult.getString("Drug_name"));
			p.setQuantity(prescriptionResult.getInt("Quantity"));
			// p.setRefills(prescriptionResult.getInt(""));   ---- IS SET TOWARDS THE BOTTOM
			p.setDateFilled(prescriptionResult.getString("Date_prescribed"));

			// Setting Drug Cost
			String prescriptionCostStatement = "SELECT i.cost FROM inventory i INNER JOIN prescription p ON i.Drug_name = p.Drug_name WHERE p.Drug_name = ?";
			PreparedStatement prescriptionCostPrepStatement = con.prepareStatement(prescriptionCostStatement);
			prescriptionCostPrepStatement.setString(1, prescriptionResult.getString("Drug_name"));

			ResultSet prescriptionCostResult = prescriptionCostPrepStatement.executeQuery();
			prescriptionCostResult.next();

			p.setCost(prescriptionCostResult.getString("cost"));


			// ----------------- verify prescription refill can be done -----------------
			String currentRefillCountQuery = "SELECT MAX(Refill_count) AS Refill_count FROM PrescriptionRefill WHERE Prescription_RXID = ?";
			PreparedStatement currentRefillCountStatement = con.prepareStatement(currentRefillCountQuery);
			currentRefillCountStatement.setInt(1, Integer.parseInt(prescriptionResult.getString("RXID")));

			ResultSet currentRefillCountResult = currentRefillCountStatement.executeQuery();
			currentRefillCountResult.next();

			if(currentRefillCountResult.getInt("Refill_count") == prescriptionResult.getInt("Quantity")) {
				throw new SQLException("Ran out of refills " + " for prescription RXID of " + p.getRxid());
			}

			// initialized this var here because if placed after line 136 the value changes due to the update
			int newRefillCount = currentRefillCountResult.getInt("Refill_count") + 1;
			// ----------------- Logging prescription refill -----------------
			String insertPrescriptionRefillQuery = "INSERT INTO PrescriptionRefill (Prescription_RXID, Refill_count, Pharmacy_ID) VALUES(?, ?, ?)";
			PreparedStatement insertPrescriptionRefillStatement = con.prepareStatement(insertPrescriptionRefillQuery);
			insertPrescriptionRefillStatement.setString(1, prescriptionResult.getString("RXID"));
			insertPrescriptionRefillStatement.setInt(2, newRefillCount);
			insertPrescriptionRefillStatement.setString(3, pharmacyResult.getString("ID"));

			int rowsAffected = insertPrescriptionRefillStatement.executeUpdate();
			if (rowsAffected == 0) {
				// prescription update failed
				model.addAttribute("message", "Prescription update failed.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

			// Setting prescription remaining refills
			p.setRefills(prescriptionResult.getInt("Quantity") - newRefillCount);

			// prescription update successful
			model.addAttribute("message", "Prescription filled.");
			model.addAttribute("prescription", p);
			return "prescription_show";
		} catch (SQLException e) {
			e.printStackTrace();
			model.addAttribute("message", "Error: " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
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