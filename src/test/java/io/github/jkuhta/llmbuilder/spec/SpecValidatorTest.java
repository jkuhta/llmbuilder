package io.github.jkuhta.llmbuilder.spec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecValidatorTest {
	private static SpecValidator.Result validate(String json) {
		SpecParser.Result parsed = SpecParser.parse(json);
		assertTrue(parsed.ok(), () -> String.join("\n", parsed.errors()));
		return SpecValidator.validate(parsed.spec(), SpecValidator.Limits.DEFAULT);
	}

	@Test
	void acceptsMinimalSpec() {
		SpecValidator.Result result = validate(SpecParserTest.MINIMAL);
		assertTrue(result.ok(), () -> String.join("\n", result.errors()));
	}

	@Test
	void rejectsUnknownMaterialWithSuggestion() {
		String json = SpecParserTest.MINIMAL.replace("\"wall\": \"bricks\"", "\"wall\": \"stone_brick\"");
		String errors = String.join("\n", validate(json).errors());
		assertTrue(errors.contains("palette.wall[0]: unknown material \"stone_brick\"; did you mean \"stone_bricks\"?"), errors);
	}

	@Test
	void rejectsRoofMaterialWithoutStairs() {
		String json = SpecParserTest.MINIMAL.replace("{ \"material\": \"dark_oak\", \"weight\": 3 }", "{ \"material\": \"white_concrete\", \"weight\": 3 }");
		String errors = String.join("\n", validate(json).errors());
		assertTrue(errors.contains("palette.roof[0]: material \"white_concrete\" has no stairs form"), errors);
	}

	@Test
	void rejectsTooManyBaysAndMissingDoorBay() {
		String json = SpecParserTest.MINIMAL.replace("\"bays\": 3, \"doors\": [{ \"bay\": 1 }]", "\"bays\": 6, \"doors\": [{ \"bay\": 7 }]");
		String errors = String.join("\n", validate(json).errors());
		assertTrue(errors.contains("facades.front.bays: 6 bays do not fit a 9 m facade (max 4)"), errors);
		assertTrue(errors.contains("facades.front.doors[0].bay: bay 7 does not exist"), errors);
	}

	@Test
	void enforcesServerSizeLimit() {
		SpecParser.Result parsed = SpecParser.parse(SpecParserTest.MINIMAL);
		SpecValidator.Result result = SpecValidator.validate(parsed.spec(), new SpecValidator.Limits(8, 64));
		assertTrue(String.join("\n", result.errors()).contains("footprint.width: 9 exceeds the server limit of 8"));
	}
}
