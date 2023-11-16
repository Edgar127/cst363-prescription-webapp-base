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

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 */
@SuppressWarnings("unused")
@Controller
public class ControllerPatientCreate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	/*
	 * Request blank patient registration form.
	 * Do not modify this method.
	 */
	@GetMapping("/patient/new")
	public String getNewPatientForm(Model model) {
		model.addAttribute("patient", new Patient());
		return "patient_register";
	}
	
	/*
	 * Process new patient registration
	 */
	@PostMapping("/patient/new")
	public String createPatient(Patient p, Model model) {
		
		System.out.println("createPatient "+p);  // debug

		// TODO

		// get a connection to the database
		// validate the doctor's last name and obtain the doctor id
		// insert the patient profile into the patient table
		// obtain the generated id for the patient and update patient object
		try (Connection con = getConnection();){
			PreparedStatement ps = con.prepareStatement("insert into patient(" +
							"ssn,first_name,last_name,birthdate,street,city,state,zip,primaryName )" +
							" values(?, ?, ?, ?, ?, ?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setString(1,p.getSsn());
			ps.setString(2,p.getFirst_name());
			ps.setString(3,p.getLast_name());
			ps.setString(4,p.getBirthdate());
			ps.setString(5,p.getStreet());
			ps.setString(6,p.getCity());
			ps.setString(7,p.getState());
			ps.setString(8,p.getZipcode());
			ps.setString(9,p.getPrimaryName());
			ps.executeUpdate();
			// obtain the generated id for the patient and update patient object
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) p.setId(rs.getInt(1));
			//p.setId(123456);
			// display message and patient information
			model.addAttribute("message", "Registration successful.");
			model.addAttribute("patient", p);
			return "patient_show";

		}catch (Exception e){
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_register";
		}

		//p.setId(123456);

	}
	
	/*
	 * Request blank form to search for patient by and and id
	 * Do not modify this method.
	 */
	@GetMapping("/patient/edit")
	public String getSearchForm(Model model) {
		model.addAttribute("patient", new Patient());
		return "patient_get";
	}
	
	/*
	 * Perform search for patient by patient id and name.
	 */
	@PostMapping("/patient/show")
	public String showPatient(Patient p, Model model) {

		System.out.println("showPatient " + p); // debug

		// TODO
		try (Connection con = getConnection();) {
			PreparedStatement ps = con.prepareStatement("select first_name,last_name,birthdate,street,city,state,zip,primaryName from patient where" +
					"birthdate =? and street =? and city=? and state =? and zip =?");
			ps.setString(1,p.getBirthdate());
			ps.setString(2,p.getStreet());
			ps.setString(3,p.getCity());
			ps.setString(4,p.getState());
			ps.setString(5,p.getZipcode());

			ResultSet rs = ps.executeQuery();
			if (rs.next()){
				p.setFirst_name(rs.getString(1));
				p.setLast_name(rs.getString(2));
				p.setPrimaryName(rs.getString(3));
				model.addAttribute("patient", p);
				System.out.println("end getPatient "+p);
				return "patient_show";

			}else{
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("patient", p);
				return "patient_get";
			}
		}catch (SQLException e){
			System.out.println("SQL error in getPatient "+e.getMessage());
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_get";
		}

		// get a connection to the database
		// using patient id and patient last name from patient object
		// retrieve patient profile and doctor's last name
		// update patient object with patient profile data

	}

	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */


	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
