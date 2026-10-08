package io.github.jkuhta.llmbuilder.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.jkuhta.llmbuilder.LlmBuilderMod;
import io.github.jkuhta.llmbuilder.config.LlmBuilderConfig;
import io.github.jkuhta.llmbuilder.generator.BuildingGenerator;
import io.github.jkuhta.llmbuilder.placement.Anchor;
import io.github.jkuhta.llmbuilder.placement.PlacementJob;
import io.github.jkuhta.llmbuilder.placement.PlacementManager;
import io.github.jkuhta.llmbuilder.spec.BuildingSpec;
import io.github.jkuhta.llmbuilder.spec.SpecParser;
import io.github.jkuhta.llmbuilder.spec.SpecValidator;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/** {@code /build} and its subcommands. */
public final class BuildCommand {
	private static final int MAX_LISTED = 4;
	/** Owner id for builds started from the console or command blocks. */
	private static final UUID CONSOLE = new UUID(0, 0);

	private final Supplier<LlmBuilderConfig> config;
	private final PlacementManager placements;
	private final SpecFiles specFiles;
	private final Executor worker;

	public BuildCommand(Supplier<LlmBuilderConfig> config, PlacementManager placements, SpecFiles specFiles, Executor worker) {
		this.config = config;
		this.placements = placements;
		this.specFiles = specFiles;
		this.worker = worker;
	}

	public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		PermissionCheck permission = new PermissionCheck.Require(
			new Permission.HasCommandLevel(PermissionLevel.byId(config.get().permissionLevel)));
		dispatcher.register(Commands.literal("build")
			.requires(Commands.hasPermission(permission))
			.then(Commands.literal("fromjson")
				.then(Commands.argument("file", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(specFiles.names(), builder))
					.executes(ctx -> fromJson(ctx, Anchor.target(ctx.getSource().getPlayerOrException())))
					.then(Commands.literal("at")
						.then(Commands.argument("pos", BlockPosArgument.blockPos())
							.then(Commands.argument("facing", StringArgumentType.word())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of("north", "east", "south", "west"), builder))
								.executes(ctx -> fromJson(ctx, positioned(ctx)))))))));
	}

	/** Explicit placement for consoles and command blocks: front-centre at pos, front facing the given way. */
	private static Anchor.Target positioned(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
		Direction front = Direction.byName(StringArgumentType.getString(ctx, "facing"));
		if (front == null || front.getAxis().isVertical()) {
			throw new SimpleCommandExceptionType(Component.literal("Facing must be north, east, south or west")).create();
		}
		return new Anchor.Target(pos, front);
	}

	private int fromJson(CommandContext<CommandSourceStack> ctx, Anchor.Target target) {
		CommandSourceStack source = ctx.getSource();
		ServerPlayer player = source.getPlayer();
		UUID owner = player != null ? player.getUUID() : CONSOLE;
		String name = StringArgumentType.getString(ctx, "file");
		if (placements.isBusy(owner)) {
			source.sendFailure(Component.literal("You already have a build in progress."));
			return 0;
		}
		Optional<String> json;
		try {
			json = specFiles.read(name);
		} catch (IOException e) {
			source.sendFailure(Component.literal("Could not read " + name + ": " + e.getMessage()));
			return 0;
		}
		if (json.isEmpty()) {
			source.sendFailure(Component.literal("No spec named \"" + name + "\". Put JSON files in " + specFiles.dir() + " or use an example."));
			return 0;
		}
		SpecParser.Result parsed = SpecParser.parse(json.get());
		if (!parsed.ok()) {
			sendList(source, "Spec " + name + " is invalid:", parsed.errors(), ChatFormatting.RED);
			return 0;
		}
		BuildingSpec spec = parsed.spec();
		SpecValidator.Result validation = SpecValidator.validate(spec, config.get().limits());
		if (!validation.ok()) {
			sendList(source, "Spec " + name + " is invalid:", validation.errors(), ChatFormatting.RED);
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Generating " + spec.name() + "...").withStyle(ChatFormatting.GRAY), false);
		CompletableFuture.supplyAsync(() -> BuildingGenerator.generate(spec), worker)
			.whenComplete((result, error) -> source.getServer().execute(() -> {
				if (error != null) {
					LlmBuilderMod.LOGGER.error("Generation of {} failed", spec.name(), error);
					source.sendFailure(Component.literal("Generation failed: " + error.getMessage()));
					return;
				}
				PlacementJob job = PlacementJob.create(owner, spec.name(), source.getLevel(), result.buffer(), target.transformFor(result.buffer()));
				if (!placements.start(job, done -> source.sendSystemMessage(
					Component.literal("Built " + done.name() + " (" + done.total() + " blocks) between "
						+ done.min().toShortString() + " and " + done.max().toShortString() + ".").withStyle(ChatFormatting.GREEN)))) {
					source.sendFailure(Component.literal("You already have a build in progress."));
					return;
				}
				source.sendSuccess(() -> Component.literal("Placing " + job.total() + " blocks..."), false);
				List<String> warnings = new java.util.ArrayList<>(validation.warnings());
				warnings.addAll(result.warnings());
				if (!warnings.isEmpty()) {
					sendList(source, warnings.size() + " warning(s):", warnings, ChatFormatting.GRAY);
				}
			}));
		return 1;
	}

	private static void sendList(CommandSourceStack source, String header, List<String> lines, ChatFormatting color) {
		source.sendSystemMessage(Component.literal(header).withStyle(color));
		lines.stream().limit(MAX_LISTED).forEach(l -> source.sendSystemMessage(Component.literal(" - " + l).withStyle(color)));
		if (lines.size() > MAX_LISTED) {
			source.sendSystemMessage(Component.literal(" ... and " + (lines.size() - MAX_LISTED) + " more (see server log)").withStyle(color));
			lines.forEach(l -> LlmBuilderMod.LOGGER.info("{} {}", header, l));
		}
	}
}
