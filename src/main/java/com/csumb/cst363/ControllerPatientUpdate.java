package com.csumb.cst363;

import java.sql.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
/*
 * Controller class for patient interactions.
 *   update patient profile.
 */
@SuppressWarnings("unused")
@Controller
public class ControllerPatientUpdate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	

	/*
	 *  Display patient profile for patient id.
	 */
	@GetMapping("/patient/edit/{id}")
	public String getUpdateForm(@PathVariable int id, Model model) {

		System.out.println("getUpdateForm "+ id );  // debug
		
		// TODO
		Patient p = new Patient();
		p.setId(id);
		// using patient id and patient last name from patient object
		// retrieve patient profile and doctor's last name
		//ps.setString(1,p.getId());
		// update patient object with patient profile data
		// get a connection to the database
		try(Connection con = getConnection();){
			PreparedStatement ps =  con.prepareStatement("select first_name,last_name,birthdate,street,city,state,zip,primaryName from patient where id=?"
					+Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1,  id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()){
				p.setFirst_name(rs.getString(1));
				p.setLast_name(rs.getString(2));
				p.setBirthdate(rs.getString(3));
				p.setStreet(rs.getString(4));
				p.setCity(rs.getString(5));
				p.setState(rs.getString(6));
				p.setZipcode(rs.getString(7));
				p.setPrimaryName(rs.getString(8));
				model.addAttribute("patient", p);
				System.out.println("end getPatient "+p);
				return "patient_show";

			}else{
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("patient", p);
				return "patient_get";
			}

		} catch (SQLException e) {

			model.addAttribute("message", "Error: " + e.getMessage());
			model.addAttribute("patient", p);
			return "index";
		}

	}
	
	
	/*
	 * Process changes to patient profile.  
	 */
	@PostMapping("/patient/edit")
	public String updatePatient(Patient p, Model model) {
		
		System.out.println("updatePatient " + p);  // for debug 
		
		// TODO

		// get a connection to the database
		// validate the doctor's last name and obtain the doctor id
		// update the patient's profile for street, city, state, zip and doctor id

		model.addAttribute("message", "Update successful.");
		model.addAttribute("patient", p);
		return "patient_show";

		// if there is error
		// model.addAttribute("message",  <error message>);
		// model.addAttribute("patient", p);
		// return "patient_edit";

	}

	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}