package com.mrbysco.itemframes;

import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.component.BoundEntityComponent;
import com.mrbysco.itemframes.component.ItemFrameComponent;
import com.mrbysco.itemframes.interaction.ItemFrameEntityInteraction;
import com.mrbysco.itemframes.interaction.ItemFrameInteraction;
import com.mrbysco.itemframes.system.ItemFrameSystems;

import javax.annotation.Nonnull;

public class ItemFramePlugin extends JavaPlugin {
	public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

	private static ItemFramePlugin instance;

	private ComponentType<EntityStore, ItemFrameComponent> itemFrameComponent;
	private ComponentType<ChunkStore, BoundEntityComponent> boundEntityComponent;

	public ItemFramePlugin(@Nonnull JavaPluginInit init) {
		super(init);
		instance = this;
	}

	public static ItemFramePlugin get() {
		return instance;
	}

	@Override
	protected void setup() {
		LOGGER.atInfo().log("Setting up plugin " + this.getName());

		this.itemFrameComponent = this.getEntityStoreRegistry().registerComponent(ItemFrameComponent.class, "ItemFrames_ItemFrame", ItemFrameComponent.CODEC);
		this.boundEntityComponent = this.getChunkStoreRegistry().registerComponent(BoundEntityComponent.class, "ItemFrames_BoundEntity", BoundEntityComponent.CODEC);

		this.getCodecRegistry(Interaction.CODEC).register("ItemFrameInteraction", ItemFrameInteraction.class, ItemFrameInteraction.CODEC);
		this.getCodecRegistry(Interaction.CODEC).register("ItemFrameEntityInteraction", ItemFrameEntityInteraction.class, ItemFrameEntityInteraction.CODEC);

		ComponentRegistryProxy<EntityStore> componentregistryproxy = this.getEntityStoreRegistry();
		componentregistryproxy.registerSystem(new ItemFrameSystems.PlaceSystem());
		componentregistryproxy.registerSystem(new ItemFrameSystems.ItemFrameTick());
	}

	public ComponentType<EntityStore, ItemFrameComponent> getItemFrameComponent() {
		return itemFrameComponent;
	}

	public ComponentType<ChunkStore, BoundEntityComponent> getBoundEntityComponent() {
		return boundEntityComponent;
	}
}