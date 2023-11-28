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
		// TODO
		// obtain connection to database.
		try (Connection con = getConnection();) {
			String pharmacyQuery = "SELECT * FROM pharmacy WHERE name = ? AND address = ?";
			PreparedStatement pharmacyStatement = con.prepareStatement(pharmacyQuery);
			pharmacyStatement.setString(1, p.getPharmacyName());
			pharmacyStatement.setString(2, p.getPharmacyAddress());

			ResultSet pharmacyResult = pharmacyStatement.executeQuery();

			if(!pharmacyResult.next()) {
				throw new SQLException("Pharmacy with Name " + p.getPharmacyName() + " and Address " + p.getPharmacyAddress() + " doesn't exists!");
			}

			String patientIdQuery = "SELECT u.id FROM Patient u WHERE Last_name = ?";
			PreparedStatement patientIdStatement = con.prepareStatement(patientIdQuery);
			patientIdStatement.setString(1, p.getPatientLastName());

			ResultSet patientIdResult = patientIdStatement.executeQuery();

			if(!patientIdResult.next()) {
				throw new SQLException("Patient with Last name " + p.getPatientLastName() + " doesn't exists");
			}

			int user_id = patientIdResult.getInt(1);

			String prescriptionQuery = "SELECT * FROM prescription WHERE rxid = ? AND patient_id = ?";
			PreparedStatement prescriptionStatement = con.prepareStatement(prescriptionQuery);
			prescriptionStatement.setInt(1, Integer.parseInt(p.getRxid()));
			prescriptionStatement.setInt(2, user_id);
			ResultSet prescriptionResult = prescriptionStatement.executeQuery();

			if(!prescriptionResult.next()) {
				throw new SQLException("Prescription with RXID " + p.getRxid() + " and Patient ID " + p.getPatient_id() + " doesn't exists");
			}

			p.setDateFilled(prescriptionResult.getString("date_prescribed"));


			String prescriptionCostStatement = "SELECT i.cost FROM inventory i INNER JOIN prescription p ON i.Drug_name = p.Drug_name WHERE p.Drug_name = ?";
			PreparedStatement prescriptionCostPrepStatement = con.prepareStatement(prescriptionCostStatement);
			prescriptionCostPrepStatement.setString(1, prescriptionResult.getString("Drug_name"));

			ResultSet prescriptionCostResult = prescriptionCostPrepStatement.executeQuery();

			if(!prescriptionCostResult.next()) {
				throw new SQLException("Prescription with RXID " + p.getRxid() + " and Patient ID " + p.getPatient_id() + " doesn't exists");
			}

			p.setCost(prescriptionCostResult.getString("cost"));


			String currentRefillCountQuery = "SELECT MAX(Refill_count) FROM PrescriptionRefill WHERE Prescription_RXID = ?";
			PreparedStatement currentRefillCountStatement = con.prepareStatement(currentRefillCountQuery);
			currentRefillCountStatement.setInt(1, Integer.parseInt(prescriptionResult.getString("rxid")));

			ResultSet currentRefillCountResult = currentRefillCountStatement.executeQuery();

			if(currentRefillCountResult.getInt("Refill_count") == prescriptionResult.getInt("Quantity_refills")) {
				throw new SQLException("Ran out of refills " + " for " + p.getRxid());
			}

			String insertQuery = "INSERT INTO PrescriptionRefill (Prescription_RXID, Refill_count, pharmacy_name, pharmacy_address) VALUES(?, ?, ?, ?)";
			PreparedStatement updateStatement = con.prepareStatement(insertQuery);
			updateStatement.setString(1, prescriptionResult.getString("rxid"));
			updateStatement.setInt(2, currentRefillCountResult.getInt("Refill_count") + 1);
			updateStatement.setString(3, pharmacyResult.getString("name"));
			updateStatement.setString(4, pharmacyResult.getString("address"));

			int rowsAffected = updateStatement.executeUpdate();
			if (rowsAffected == 0) {
//									prescription update successful
				model.addAttribute("message", "Prescription update failed.");
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}

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