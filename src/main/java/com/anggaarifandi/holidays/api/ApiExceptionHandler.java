package com.anggaarifandi.holidays.api;

import java.net.URI;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.anggaarifandi.holidays.ai.AiUnavailableException;
import com.anggaarifandi.holidays.client.CountryNotFoundException;
import com.anggaarifandi.holidays.client.UpstreamException;
import com.anggaarifandi.holidays.config.AppProperties;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private final AppProperties appProperties;

	public ApiExceptionHandler(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@ExceptionHandler(InvalidRequestException.class)
	ProblemDetail handleInvalid(InvalidRequestException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", ex.getMessage(),
				"validation", request);
	}

	@ExceptionHandler({
			ConstraintViolationException.class,
			MethodArgumentNotValidException.class,
			HandlerMethodValidationException.class,
			MissingServletRequestParameterException.class
	})
	ProblemDetail handleValidation(Exception ex, HttpServletRequest request) {
		String detail;
		if (ex instanceof ConstraintViolationException cve) {
			detail = cve.getConstraintViolations().stream()
					.map(v -> v.getPropertyPath() + ": " + v.getMessage())
					.collect(Collectors.joining("; "));
		}
		else if (ex instanceof MethodArgumentNotValidException manv) {
			detail = manv.getBindingResult().getFieldErrors().stream()
					.map(err -> err.getField() + ": " + err.getDefaultMessage())
					.collect(Collectors.joining("; "));
		}
		else {
			detail = ex.getMessage();
		}
		return problem(HttpStatus.BAD_REQUEST, "Validation Failed", detail, "validation", request);
	}

	@ExceptionHandler(CountryNotFoundException.class)
	ProblemDetail handleCountry(CountryNotFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Country Not Found", ex.getMessage(),
				"country-not-found", request);
	}

	@ExceptionHandler(UnauthorizedException.class)
	ProblemDetail handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(),
				"unauthorized", request);
	}

	@ExceptionHandler(RateLimitExceededException.class)
	ProblemDetail handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
		ProblemDetail pd = problem(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded",
				ex.getMessage(), "rate-limit", request);
		pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
		return pd;
	}

	@ExceptionHandler(UpstreamException.class)
	ProblemDetail handleUpstream(UpstreamException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_GATEWAY, "Upstream Error",
				"Failed to retrieve holiday data from Nager.Date: " + ex.getMessage(),
				"upstream", request);
	}

	@ExceptionHandler(AiUnavailableException.class)
	ProblemDetail handleAi(AiUnavailableException ex, HttpServletRequest request) {
		ProblemDetail pd = problem(HttpStatus.SERVICE_UNAVAILABLE, "AI Unavailable",
				ex.getMessage(), "ai-unavailable", request);
		pd.setProperty("setup", ex.getSetupSteps());
		return pd;
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ProblemDetail handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), "not-found", request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
		String detail = appProperties.error().includeDetail() && ex.getMessage() != null
				? ex.getMessage()
				: "Unexpected error";
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
				detail, "internal", request);
	}

	private ProblemDetail problem(
			HttpStatus status,
			String title,
			String detail,
			String typeSuffix,
			HttpServletRequest request
	) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(title);
		pd.setType(URI.create(appProperties.error().problemType(typeSuffix)));
		pd.setInstance(URI.create(request.getRequestURI()));
		return pd;
	}

}
