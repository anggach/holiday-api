package com.anggaarifandi.holidays.ai;

import java.util.List;

public class AiUnavailableException extends RuntimeException {

	private final List<String> setupSteps;

	public AiUnavailableException(String message, List<String> setupSteps) {
		super(message);
		this.setupSteps = List.copyOf(setupSteps);
	}

	public List<String> getSetupSteps() {
		return setupSteps;
	}

}
