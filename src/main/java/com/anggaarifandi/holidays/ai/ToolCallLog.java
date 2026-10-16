package com.anggaarifandi.holidays.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ToolCallLog {

	private final LinkedHashSet<String> names = new LinkedHashSet<>();

	public void record(String name) {
		if (name != null && !name.isBlank()) {
			names.add(name);
		}
	}

	public List<String> names() {
		return new ArrayList<>(names);
	}

}
