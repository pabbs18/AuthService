package io.ps.auth;

import io.ps.auth.dto.ApiResponse;
import io.ps.auth.dto.SignUpRequest;
import io.ps.auth.dto.UserSummary;
import io.ps.auth.exception.BadRequestException;
import io.ps.auth.exception.EmailAlreadyExistsException;
import io.ps.auth.exception.UserNameAlreadyExistsException;
import io.ps.auth.filters.JwtRequestFilter;
import io.ps.auth.messaging.UserEventChannel;
import io.ps.auth.model.InstaUserDetails;
import io.ps.auth.model.Profile;
import io.ps.auth.model.User;
import io.ps.auth.models.AuthenticationRequest;
import io.ps.auth.models.AuthenticationResponse;
import io.ps.auth.service.UserService;
import io.ps.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableMongoAuditing
@EnableBinding(UserEventChannel.class)
@EnableAutoConfiguration
@SecurityScheme(name = "Auth-api", scheme = "bearer", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)

@OpenAPIDefinition(info = @Info(title = "User API", version = "2.0", description = "User Details"))
public class SpringSecurityJwtApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityJwtApplication.class, args);
	}

}

@RestController
@RequestMapping("/auth")
@Slf4j
class HelloWorldController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtTokenUtil;

	@Autowired
	private MyUserDetailsService userDetailsService;

	@Autowired
    UserService userService;



	@PostMapping(value = "/authenticate")
	public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
			System.out.println("Entered POST method");
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword())
			);

			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		catch (BadCredentialsException e) {
			throw new Exception("Incorrect username or password", e);
		}


		var userDetails = userDetailsService
				.loadUserByUsername(authenticationRequest.getUsername());

		log.info("User details of :",userDetails.getUsername());

		final String jwt = jwtTokenUtil.generateToken(userDetails);

		return ResponseEntity.ok(new AuthenticationResponse(jwt));
	}

	@PostMapping(value = "/signup", name = "Create User")
	public ResponseEntity<?> createUser(@RequestBody SignUpRequest signUpRequest){
		log.info("Creating user................... {}", signUpRequest.getUsername());
		System.out.println("Creating USER");

		User user = User.builder()
				.username(signUpRequest.getUsername())
				.password(signUpRequest.getPassword())
				.email(signUpRequest.getEmail())
				.userProfile(Profile.builder()
						.displayName(signUpRequest.getName())
						.build())
				.build();
		try{
			User createdUser = userService.registerUser(user);

		}catch(UserNameAlreadyExistsException | EmailAlreadyExistsException e){
			throw new BadRequestException(e.getMessage());
		}

		URI uri = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/{username}")
				.buildAndExpand(user.getUsername()).toUri();

		return ResponseEntity.created(uri)
				.body(new ApiResponse(true, "User Registered Successfully"));
	}

	@SecurityRequirement(name = "Auth-api")
	@GetMapping(value = "/users/{username}", produces = MediaType.APPLICATION_JSON_VALUE, name = "Find user by username")
	public ResponseEntity<?> findUser(@PathVariable("username") String username){
		User user = userService.findByUserName(username);
		return ResponseEntity.ok(user);
	}

	@SecurityRequirement(name = "Auth-api")
	@GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE, name = "Find all users")
	public ResponseEntity<?> findAllUsers(){
		log.info("Retrieving users");
		return ResponseEntity.ok(userService.findAll());
	}

	@SecurityRequirement(name = "Auth-api")
	@GetMapping(value = "/users/me", produces = MediaType.APPLICATION_JSON_VALUE, name = "Get current user")
	@PreAuthorize("hasRole('USER')")
	@ResponseStatus(HttpStatus.OK)
	public UserSummary getCurrentUser(@AuthenticationPrincipal InstaUserDetails userDetails){
		return UserSummary.builder()
				.id(userDetails.getId())
				.username(userDetails.getUsername())
				.name(userDetails.getUserProfile().getDisplayName())
				.profilePicture(userDetails.getUserProfile().getDisplayPictureUrl())
				.build();
	}

	@SecurityRequirement(name = "Auth-api")
	@GetMapping(value = "users/summary/{username}", produces = MediaType.APPLICATION_JSON_VALUE, name = "Get user summary for a user")
	public ResponseEntity<?> getUserSummary(@PathVariable String username){
		log.info("Retrieving summary for ", username);
		User user = userService.findByUserName(username);
		return ResponseEntity.ok(convertToUserSummary(user));
	}

	@SecurityRequirement(name = "Auth-api")
	@GetMapping(value = "users/summaries", produces = MediaType.APPLICATION_JSON_VALUE, name = "Get summaries for a list of users")
	public ResponseEntity<?> getUserSummaries(@RequestBody List<String> usernames){
		log.info("Retrieving summaries for {} users ", usernames.size());

		List<UserSummary> userSummaries = userService.findByUserNameIn(usernames)
				.stream()
				.map(HelloWorldController::convertToUserSummary)
				.collect(Collectors.toList());
		return ResponseEntity.ok(userSummaries);
	}

	private static UserSummary convertToUserSummary(User user) {
		return UserSummary.builder()
				.id(user.getId())
				.username(user.getUsername())
				.name(user.getUserProfile().getDisplayName())
				.profilePicture(user.getUserProfile().getDisplayPictureUrl())
				.build();
	}

	@SecurityRequirement(name = "Auth-api")
	@PutMapping(value = "/users/me/picture", name = "Update profile picture")
	@PreAuthorize("hasRole('USER')")
	public ResponseEntity<?> updateProfilePicture(@RequestBody String profilePicLocation,
												  @AuthenticationPrincipal InstaUserDetails userDetails){
		userService.updateProfilePicture(profilePicLocation, userDetails.getId());

		return ResponseEntity.ok()
				.body(new ApiResponse(true, "Profile picture updated successfully"));
	}
}

@EnableWebSecurity
class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	private UserDetailsService myUserDetailsService;
	@Autowired
	private JwtRequestFilter jwtRequestFilter;

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(myUserDetailsService);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return NoOpPasswordEncoder.getInstance();
	}

	@Override
	@Bean
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Override
	protected void configure(HttpSecurity httpSecurity) throws Exception {
		httpSecurity.csrf().disable()
				.authorizeRequests().antMatchers("/auth/authenticate", "/auth/signup", "swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui*/**").permitAll().

						anyRequest().authenticated().and().
						exceptionHandling().and().sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

	}

}