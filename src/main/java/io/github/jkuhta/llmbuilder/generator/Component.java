package io.github.jkuhta.llmbuilder.generator;

/** One composable step of the generator. Components run in a fixed order and write into the buffer. */
@FunctionalInterface
public interface Component {
	void apply(BuildContext ctx);
}
