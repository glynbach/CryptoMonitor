package com.kieral.cryptomon;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.Model;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(value = {Exception.class, RuntimeException.class})
    public String defaultErrorHandler(HttpServletRequest request, Exception e, Model model) {
    	model.asMap().remove("userObj");
        model.addAttribute("datetime", new Date());
        model.addAttribute("exception", e);
        model.addAttribute("url", request.getRequestURL());
        return "/error";
    }
    
    @ExceptionHandler(value = {HttpSessionRequiredException.class})
    public String sessionErrorHandler(HttpServletRequest request, Exception e, Model model) {
    	model.asMap().remove("userObj");
    	if (request.getSession() != null)
    		request.getSession().invalidate();
        model.addAttribute("datetime", new Date());
        model.addAttribute("exception", "Session has timed out - please log back in");
        model.addAttribute("url", request.getRequestURL());
        return "/sessionTimeOut";
    }
}
