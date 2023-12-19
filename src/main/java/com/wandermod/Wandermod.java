package com.wandermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wandermod implements ModInitializer {
	static final String MOD_NAME = "wandermod";
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	// Define items
	public static final AgentWand AGENT_WAND = new AgentWand(new FabricItemSettings().maxCount(1));

	// Define blocks
	public static final AgentPlayer AGENT_PLAYER  = new AgentPlayer(FabricBlockSettings.create().strength(4.0f));

	// Define block entities
	    public static final BlockEntityType<AgentPlayerEntity> AGENT_PLAYER_ENTITY = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        new Identifier(MOD_NAME, "agent_player_entity"),
        FabricBlockEntityTypeBuilder.create(AgentPlayerEntity::new, AGENT_PLAYER).build()
    );

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		// Register new items
		Registry.register(Registries.ITEM, new Identifier(MOD_NAME, "agent_wand"), AGENT_WAND);
        Registry.register(Registries.ITEM, new Identifier(MOD_NAME, "agent_player"), new BlockItem(AGENT_PLAYER, new FabricItemSettings()));

		// Register new blocks
		Registry.register(Registries.BLOCK, new Identifier(MOD_NAME, "agent_player"), AGENT_PLAYER);

	}
}