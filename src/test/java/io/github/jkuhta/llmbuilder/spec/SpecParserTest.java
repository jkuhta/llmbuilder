package io.github.jkuhta.llmbuilder.spec;

import io.github.jkuhta.llmbuilder.spec.BuildingSpec.Palette.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecParserTest {
	static final String MINIMAL = """
		{
		  "name": "Test house",
		  "footprint": { "width": 9, "depth": 11 },
		  "floors": { "count": 2, "heights": [4] },
		  "roof": { "type": "gable" },
		  "palette": { "wall": "bricks", "roof": [{ "material": "dark_oak", "weight": 3 }, { "material": "spruce", "weight": 1 }] },
		  "facades": { "front": { "bays": 3, "doors": [{ "bay": 1 }] } }
		}
		""";

	@Test
	void parsesMinimalSpecWithDefaults() {
		SpecParser.Result result = SpecParser.parse(MINIMAL);
		assertTrue(result.ok(), () -> String.join("\n", result.errors()));
		BuildingSpec spec = result.spec();
		assertEquals(java.util.List.of(4, 4), spec.floors().heights());
		assertEquals("bricks", spec.palette().primary(Role.WALL));
		assertEquals("bricks", spec.palette().primary(Role.TRIM));
		assertEquals(2, spec.palette().variants(Role.ROOF).size());
		assertEquals(3, spec.facade(Side.BACK).bays());
		assertTrue(spec.facade(Side.BACK).doors().isEmpty());
		assertEquals(BuildingSpec.Roof.Ridge.AUTO, spec.roof().ridge());
	}

	@Test
	void reportsAllErrorsWithPaths() {
		String bad = """
			{
			  "footprint": { "width": 3, "depth": 11, "colour": "red" },
			  "floors": { "count": 2 },
			  "roof": { "type": "pagoda" },
			  "palette": { "wall": "bricks" },
			  "facades": { "front": { "bays": "three" } }
			}
			""";
		SpecParser.Result result = SpecParser.parse(bad);
		assertTrue(!result.ok());
		String all = String.join("\n", result.errors());
		assertTrue(all.contains("footprint.width: must be between 5 and 64 (got 3)"), all);
		assertTrue(all.contains("footprint.colour: unknown field"), all);
		assertTrue(all.contains("roof.type: must be one of gable, hip, mansard, flat, stepped_gable, dome (got \"pagoda\")"), all);
		assertTrue(all.contains("palette.roof: missing required field"), all);
		assertTrue(all.contains("facades.front.bays: must be an integer"), all);
	}

	@Test
	void rejectsMalformedJson() {
		SpecParser.Result result = SpecParser.parse("{ not json");
		assertTrue(result.errors().get(0).startsWith("invalid JSON"));
	}
}
