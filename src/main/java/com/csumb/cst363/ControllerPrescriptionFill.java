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
			// obtain pharmacy ID
			String pharmacyQuery = "SELECT id FROM pharmacy WHERE name = ? AND address = ?";
			//executes a SQL query to find a pharmacy's ID based on provided pharmacyName and pharmacyAddress
			try (PreparedStatement pharmacyStatement = con.prepareStatement(pharmacyQuery)) {
				pharmacyStatement.setString(1, p.getPharmacyName());
				pharmacyStatement.setString(2, p.getPharmacyAddress());
				ResultSet pharmacyResult = pharmacyStatement.executeQuery();
				if (pharmacyResult.next()) {
					String pharmacyName = pharmacyResult.getString("pharmacy_name");
					String pharmacyAddress = pharmacyResult.getString("pharmacy_address");

					// get prescription information for the rxid value and patient last name from prescription object.
					String rxid = p.getRxid();
					int patientId = p.getPatient_id();

					String prescriptionQuery = "SELECT * FROM prescription WHERE rxid = ? AND patient_id = ?";
					try (PreparedStatement prescriptionStatement = con.prepareStatement(prescriptionQuery)) {
						prescriptionStatement.setString(1, p.getRxid());
						prescriptionStatement.setInt(2, patientId);
						ResultSet prescriptionResult = prescriptionStatement.executeQuery();
						if (prescriptionResult.next()) {
//							Set dateFilled and cost retrieved from the database to the Prescription object
							p.setDateFilled(prescriptionResult.getString("date_prescribed"));

							String prescriptionCostStatement = "SELECT i.cost FROM inventory i INNER JOIN prescription p ON i.Drug_name = p.Drug_name WHERE p.Drug_name = ?";
							try(PreparedStatement prescriptionCostPrepStatement = con.prepareStatement(prescriptionCostStatement)) {
								ResultSet prescriptionCostResult = prescriptionCostPrepStatement.executeQuery();

								p.setCost(prescriptionCostResult.getString("cost"));
							} catch (SQLException e) {
								e.printStackTrace();
								model.addAttribute("message", "Error: " + e.getMessage());
								model.addAttribute("prescription", p);
								return "prescription_fill";
							}

							// update prescription table row with pharmacy id, fill date.
							String insertQuery = "INSERT PrescriptionRefill SET pharmacy_name = ?, pharmacy_address = ?, refill_date = ?, refill_count = ? WHERE prescription_rxid = ?";
							try (PreparedStatement updateStatement = con.prepareStatement(insertQuery)) {
								updateStatement.setString(1, pharmacyName);
								updateStatement.setString(2, pharmacyAddress);
								updateStatement.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
								updateStatement.setInt(4, prescriptionResult.getInt("quantity_refills") - 1);
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


