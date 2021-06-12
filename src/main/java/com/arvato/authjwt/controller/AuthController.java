package com.arvato.authjwt.controller;

import java.beans.Encoder;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.aspectj.bridge.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arvato.authjwt.models.ERole;
import com.arvato.authjwt.models.Role;
import com.arvato.authjwt.models.User;
import com.arvato.authjwt.payload.request.LoginRequest;
import com.arvato.authjwt.payload.request.SignupRequest;
import com.arvato.authjwt.payload.response.JwtResponse;
import com.arvato.authjwt.payload.response.MessageResponse;
import com.arvato.authjwt.repository.RoleRepository;
import com.arvato.authjwt.repository.UserRepository;
import com.arvato.authjwt.security.jwt.JwtUtils;
import com.arvato.authjwt.security.services.UserDetailsImpl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private RoleRepository roleRepo;
	
	@Autowired
	private PasswordEncoder passEncoder;
	
	@Autowired
	private JwtUtils jwtUtils;
	
	@PostMapping("/sigin")
	public ResponseEntity<?> authUser(@Valid @RequestBody LoginRequest login) {
		Authentication auth = authManager.authenticate(
				new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));
		
		SecurityContextHolder.getContext().setAuthentication(auth);
		String jwt = null;
		
		try {
			jwt = jwtUtils.generateJwtToken(auth);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println("Error generating JWT Token");
		}
		
		UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
		
		List<String> roles = userDetails.getAuthorities()
								.stream()
								.map( item -> item.getAuthority())
								.collect(Collectors.toList());
	
		return ResponseEntity.ok( new JwtResponse(
										jwt,
										userDetails.getId(),
										userDetails.getUsername(),
										userDetails.getEmail(),
										roles));
	}
	
	@PostMapping("signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signup){
		
		String errRoleNotFound = "Error: Role is not found";

		if (userRepo.existsByUsername(signup.getUsername())){
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Username is already taken!"));
		}
		
		if(userRepo.existsByEmail(signup.getEmail())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Email is already in use!"));
		}
		
		//Create new user's account
		User user = new User(signup.getUsername(),
							 signup.getEmail(),
							 passEncoder.encode(signup.getPassword()));
		
		Set<String> strRoles = signup.getRole();
		Set<Role> roles = new HashSet<Role>();
		
		if (strRoles == null ) {
			Role userRole = roleRepo.findByName(ERole.ROLE_USER)
										.orElseThrow( () -> new RuntimeException(errRoleNotFound));
			roles.add(userRole);
		}else {
			
			strRoles.forEach( role -> {
				switch (role) {
				case "admin":

					Role adminRole = roleRepo.findByName(ERole.ROLE_ADMIN)
												.orElseThrow(() -> new RuntimeException(errRoleNotFound));
					roles.add(adminRole);
					break;
					
				case "mod":
					
					Role modRole = roleRepo.findByName(ERole.ROLE_MODERATOR)
												.orElseThrow(() -> new RuntimeException(errRoleNotFound));
					
					roles.add(modRole);
					
					break;

				default:
					
					Role userRole = roleRepo.findByName(ERole.ROLE_USER)
												.orElseThrow(() -> new RuntimeException(errRoleNotFound) );
					break;
				}
			});
		}
		
		user.setRoles(roles);
		userRepo.save(user);
		
		return ResponseEntity.ok( new MessageResponse("User registered successfully!"));
	}
	
	
}
