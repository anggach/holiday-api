package com.anggaarifandi.holidays.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.stereotype.Service;

import com.anggaarifandi.holidays.api.InvalidRequestException;

@Service
public class ChatService {

	static final String SYSTEM_PROMPT = """
			You are a public-holidays assistant backed by deterministic tools.

			Capabilities (ONLY these):
			1) Recent/last celebrated public holidays for one country
			2) Weekday (Mon-Fri) public holiday counts for one or more countries in a year
			3) Shared public holiday dates between exactly two countries in a year

			Rules:
			- Reply in the same language as the user (English, Indonesian, Dutch, or others).
			- When calling tools, always use ISO 3166-1 alpha-2 country codes (NL not Netherlands/Belanda).
			- Never invent dates, counts, or holiday names. Use tool results only.
			- If the question is outside the three capabilities, do NOT call tools. Politely refuse and briefly list what you can do, in the user's language.
			- If country or year is missing and required, ask a clarifying question without calling tools.
			- Do not answer weather, travel, visa, politics, or general knowledge questions.
			""";

	private final ChatClient chatClient;
	private final OllamaAvailabilityService availabilityService;
	private final ToolCallLog toolCallLog;

	public ChatService(
			ChatClient.Builder chatClientBuilder,
			HolidayTools holidayTools,
			OllamaAvailabilityService availabilityService,
			ToolCallLog toolCallLog
	) {
		this.availabilityService = availabilityService;
		this.toolCallLog = toolCallLog;
		this.chatClient = chatClientBuilder
				.defaultSystem(SYSTEM_PROMPT)
				.defaultTools(holidayTools)
				.build();
	}

	public ChatResponseDto chat(String message) {
		availabilityService.assertReady();
		if (message == null || message.isBlank()) {
			throw new InvalidRequestException("message must not be blank");
		}

		ChatResponse chatResponse = chatClient.prompt()
				.user(message.trim())
				.call()
				.chatResponse();

		String reply = extractText(chatResponse);
		List<String> toolsUsed = toolCallLog.names();
		if (toolsUsed.isEmpty()) {
			toolsUsed = extractToolNames(chatResponse);
		}
		boolean refused = toolsUsed.isEmpty() && looksLikeRefusal(reply);
		return new ChatResponseDto(reply, toolsUsed, refused);
	}

	private static String extractText(ChatResponse chatResponse) {
		if (chatResponse == null || chatResponse.getResult() == null) {
			return "";
		}
		Generation generation = chatResponse.getResult();
		AssistantMessage output = generation.getOutput();
		return output == null || output.getText() == null ? "" : output.getText();
	}

	private static List<String> extractToolNames(ChatResponse chatResponse) {
		Set<String> names = new LinkedHashSet<>();
		if (chatResponse == null || chatResponse.getResults() == null) {
			return List.of();
		}
		for (Generation generation : chatResponse.getResults()) {
			AssistantMessage output = generation.getOutput();
			if (output != null && output.getToolCalls() != null) {
				output.getToolCalls().forEach(call -> names.add(call.name()));
			}
		}
		// Also scan metadata conversation history if present
		Object history = chatResponse.getMetadata() != null
				? chatResponse.getMetadata().get("springAiChatClientMessages")
				: null;
		if (history instanceof List<?> messages) {
			for (Object item : messages) {
				if (item instanceof ToolResponseMessage toolMessage) {
					toolMessage.getResponses().forEach(r -> names.add(r.name()));
				}
				else if (item instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
					assistant.getToolCalls().forEach(call -> names.add(call.name()));
				}
				else if (item instanceof Message) {
					// ignore
				}
			}
		}
		return new ArrayList<>(names);
	}

	private static boolean looksLikeRefusal(String reply) {
		if (reply == null || reply.isBlank()) {
			return true;
		}
		String lower = reply.toLowerCase();
		return lower.contains("only help") || lower.contains("cannot") || lower.contains("can't")
				|| lower.contains("tidak bisa") || lower.contains("hanya bisa")
				|| lower.contains("kan alleen") || lower.contains("out of scope");
	}

	public record ChatResponseDto(String reply, List<String> toolsUsed, boolean refused) {
	}

}
