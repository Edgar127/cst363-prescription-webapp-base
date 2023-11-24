package com.csumb.cst363;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
			PreparedStatement ps =  con.prepareStatement("select first_name,last_name,Birth_date ,street,city,state,Zip_code,Primary_name  from patient where id=?"
					,Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1,  id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()){
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				LocalDate birthDate = null;

				String firstName = rs.getString(1);
				String lastName = rs.getString(2);
				String birthDateStr = rs.getString(3);
				String street = rs.getString(4);
				String city = rs.getString(5);
				String state = rs.getString(6);
				String zipCode = rs.getString(7);
				String primaryName = rs.getString(8);

				if (birthDateStr != null && !birthDateStr.isEmpty()) {
					birthDate = LocalDate.parse(birthDateStr, formatter);
				}

				p.setFirst_name(firstName != null ? firstName : "");
				p.setLast_name(lastName != null ? lastName : "");

				p.setFirst_name(rs.getString(1));
				p.setLast_name(rs.getString(2));

				if (birthDate != null) {
					p.setBirthdate(birthDate.toString());
				} else {
					p.setBirthdate(null);
				}

				p.setStreet(street != null ? street : "");
				p.setCity(city != null ? city : "");
				p.setState(state != null ? state : "");
				p.setZipcode(zipCode != null ? zipCode : "");
				p.setPrimaryName(primaryName != null ? primaryName : "");

				model.addAttribute("patient", p);
				//System.out.println("end getPatient "+p);
				return "patient_edit";

			}else{
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("patient", p);
				return "patient_get";
			}

		} catch (SQLException e) {

			model.addAttribute("message", "Error: " + e.getMessage());
			model.addAttribute("patient", p);
			return "patient_get";
		}

	}
	
	
	/*
	 * Process changes to patient profile.  
	 */
	@PostMapping("/patient/edit")
	public String updatePatient(Patient p, Model model) {
		// get a connection to the database
		// validate the doctor's last name and obtain the doctor id
		// update the patient's profile for street, city, state, zip and doctor id

		// if there is error
		// model.addAttribute("message",  <error message>);
		// model.addAttribute("patient", p);
		// return "patient_edit";



		try (Connection con = getConnection();) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate birthDate = null;

			if (p.getBirthdate() != null && !p.getBirthdate().isEmpty()) {
				birthDate = LocalDate.parse(p.getBirthdate(), formatter);
			}

			PreparedStatement ps = con.prepareStatement("update patient set First_name = ?, Last_name = ?, Birth_date =?, street =?, city=?, state =?, Zip_code =?, Primary_name = ? where id = ?");
			ps.setString(1,p.getFirst_name());
			ps.setString(2,p.getLast_name());

			if (birthDate != null) {
				ps.setString(3, birthDate.toString());
			} else {
				ps.setString(3, null);
			}

			ps.setString(4,p.getStreet());
			ps.setString(5,p.getCity());
			ps.setString(6, p.getState());
			ps.setString(7, p.getZipcode());
			ps.setString(8, p.getPrimaryName());
			ps.setInt(9, p.getId());

			int rc = ps.executeUpdate();
			if (rc==1){
				model.addAttribute("message", "Update successful.");
				model.addAttribute("patient", p);
				return "patient_show";
			}else{
				model.addAttribute("message", "Error. Update was not successful");
				model.addAttribute("patient", p);
				return "patient_edit";
			}

		}catch (SQLException e){
			model.addAttribute("message", "SQL Error." + e.getMessage());
			model.addAttribute("patient", p);
			return "patient_edit";
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
