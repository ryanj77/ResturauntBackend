package com.techelevator.controller;

import javax.validation.Valid;

import com.techelevator.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.techelevator.dao.UserDao;
import com.techelevator.security.jwt.JWTFilter;
import com.techelevator.security.jwt.TokenProvider;

@RestController
@CrossOrigin
public class AuthenticationController {

    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private UserDao userDao;

    public AuthenticationController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder, UserDao userDao) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userDao = userDao;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginDTO loginDto) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.createToken(authentication, false);
        
        User user = userDao.findByUsername(loginDto.getUsername());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(new LoginResponse(jwt, user), httpHeaders, HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public void register(@Valid @RequestBody RegisterUserDTO newUser) throws PasswordValidationException {
        try {
            User userName = userDao.findByUsername(newUser.getUsername());
            if (userName != null) {
                throw new UserAlreadyExistsException();
            }
            User userEmail = userDao.findByEmail(newUser.getEmail());
            if (userEmail != null) {
                throw new UserAlreadyExistsException();
            }
        } catch (UsernameNotFoundException e) {
            if(validatePassword(newUser.getPassword())!= false) {
                userDao.create(newUser.getUsername(), newUser.getEmail(), newUser.getPassword(), newUser.getRole());
            }else
                throw new PasswordValidationException();
        }
    }

    static boolean validatePassword(String password) {
    if(password.length() > 7){
        if(checkPassword(password)){
            return true;
        }else{
            return false;
        }
    }else{
        System.out.println("Password too short.");
        return false;
    }
    }

    static boolean checkPassword(String password) {
       boolean hasNum = false; boolean hasCap = false; boolean hasLow = false; char c;
       for(int i = 0; i < password.length(); i++){
           c=password.charAt(i);
           if(Character.isDigit(c)){
               hasNum = true;
           }
           else if(Character.isUpperCase(c)){
               hasCap = true;
           }
           else if(Character.isLowerCase(c)){
               hasLow = true;
           }
           if(hasNum && hasCap && hasLow){
               return true;
           }
       }
       return false;
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    static class LoginResponse {

        private String token;
        private User user;

        LoginResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }

        @JsonProperty("token")
        String getToken() {
            return token;
        }

        void setToken(String token) {
            this.token = token;
        }

        @JsonProperty("user")
		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}
    }
}

