package com.csumb.cst363;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


import static java.sql.DriverManager.getConnection;


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
//TODO
		// obtain connection to database
		try (Connection con = getConnection();) {
			// valid pharmacy name and address in the prescription object
			String pharmacyName = p.getPharmacyName();
			String pharmacyAddress = p.getPharmacyAddress();

			// obtain pharmacy ID
			String pharmacyQuery = "SELECT id FROM pharmacy WHERE name = ? AND address = ?";
			//executes a SQL query to find a pharmacy's ID based on provided pharmacyName and pharmacyAddress
			try (PreparedStatement pharmacyStatement = con.prepareStatement(pharmacyQuery)) {
				pharmacyStatement.setString(1, pharmacyName);
				pharmacyStatement.setString(2, pharmacyAddress);
				ResultSet pharmacyResult = pharmacyStatement.executeQuery();
				if (pharmacyResult.next()) {
					int pharmacyId = pharmacyResult.getInt("id");

					// get prescription information for the rxid value and patient last name from prescription object.
					String rxid = p.getRxid();
					String patientLastName = p.getPatientLastName();

					String prescriptionQuery = "SELECT * FROM prescription WHERE rxid = ? AND patient_last_name = ?";
					try (PreparedStatement prescriptionStatement = con.prepareStatement(prescriptionQuery)) {
						prescriptionStatement.setString(1, p.getRxid());
						prescriptionStatement.setString(2, patientLastName);
						ResultSet prescriptionResult = prescriptionStatement.executeQuery();
						if (prescriptionResult.next()) {
//							Set dateFilled and cost retrieved from the database to the Prescription object
							p.setDateFilled(prescriptionResult.getString("dateFilled"));
							p.setCost(prescriptionResult.getString("cost"));

							// update prescription table row with pharmacy id, fill date.
							String updateQuery = "UPDATE prescription SET pharmacy_id = ?, fill_date = ? WHERE rxid = ?";
							try (PreparedStatement updateStatement = con.prepareStatement(updateQuery)) {
								updateStatement.setInt(1, pharmacyId);
								updateStatement.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
								updateStatement.setString(3, rxid);
								int rowsAffected = updateStatement.executeUpdate();
								if (rowsAffected == 1) {
//									prescription update successful
									model.addAttribute("message", "Prescription filled.");
									model.addAttribute("prescription", p);
									return "prescription_show";
								} else {
									// prescription update fail
									model.addAttribute("message", "Prescription update failed.");
								}
							}
						} else {
							model.addAttribute("message", "Prescription not found.");
						}
					}
				} else {
					model.addAttribute("message", "Pharmacy not found.");
				}
			} catch (SQLException e) {
				// Handle SQLException
				e.printStackTrace();
				model.addAttribute("message", "Error: " + e.getMessage());
				model.addAttribute("prescription", p);
				return "prescription_fill";
			}
		} catch (SQLException e) {
			// Handle SQLException
			e.printStackTrace();
			model.addAttribute("message", "Error: " + e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_fill";
		}
		model.addAttribute("prescription", p);
		return "prescription_fill";
	}
	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
}


