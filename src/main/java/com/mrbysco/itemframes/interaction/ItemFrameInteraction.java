package com.mrbysco.itemframes.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.ItemFramePlugin;
import com.mrbysco.itemframes.component.BoundEntityComponent;
import com.mrbysco.itemframes.component.ItemFrameComponent;
import com.mrbysco.itemframes.util.FrameUtil;
import com.mrbysco.itemframes.util.ItemHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemFrameInteraction extends SimpleBlockInteraction {
	public static final BuilderCodec<ItemFrameInteraction> CODEC = BuilderCodec.builder(
					ItemFrameInteraction.class, ItemFrameInteraction::new, SimpleBlockInteraction.CODEC
			)
			.documentation("Interacts with an Item Frame")
			.build();

	@Nonnull
	@Override
	public WaitForDataFrom getWaitForDataFrom() {
		return super.getWaitForDataFrom();
	}

	@Override
	protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                 @Nonnull InteractionType type, @Nonnull InteractionContext context,
	                                 @Nullable ItemStack itemStack, @Nonnull Vector3i targetBlock,
	                                 @Nonnull CooldownHandler cooldownHandler) {
		ItemStack itemstack = context.getHeldItem();

		int x = targetBlock.getX();
		int y = targetBlock.getY();
		int z = targetBlock.getZ();
		long indexChunk = ChunkUtil.indexChunkFromBlock(x, z);
		WorldChunk worldchunk = world.getChunk(indexChunk);
		BlockType blockType = worldchunk.getBlockType(targetBlock);
		var chunkRef = worldchunk.getBlockComponentEntity(x, y, z);
		if (chunkRef == null) {
			chunkRef = BlockModule.ensureBlockEntity(worldchunk, x, y, z);
		}

		Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

		BoundEntityComponent boundEntityComponent = chunkStore.getComponent(chunkRef, ItemFramePlugin.get().getBoundEntityComponent());
		if (boundEntityComponent != null && FrameUtil.isItemFrame(blockType.getId())) {
			if (boundEntityComponent.getAttachedEntity() != null) {
				Ref<EntityStore> boundRef = world.getEntityRef(boundEntityComponent.getAttachedEntity());
				if (boundRef == null) {
					context.getState().state = InteractionState.Failed;
					return;
				}

				ItemFrameComponent frameComponent = commandBuffer.getComponent(boundRef, ItemFrameComponent.getComponentType());
				if (frameComponent == null) {
					context.getState().state = InteractionState.Failed;
					return;
				}
				int yawDegrees = frameComponent.getFrameRotation();

				if (frameComponent.getHeldStack() == null) {
					if (itemstack == null || itemstack.isEmpty()) {
						context.getState().state = InteractionState.Failed;
					} else {
						ItemStack stackClone = itemstack.withQuantity(1);
						if (context.getHeldItemContainer() != null) {
							ItemStackSlotTransaction itemstackslottransaction = context.getHeldItemContainer()
									.removeItemStackFromSlot(context.getHeldItemSlot(), itemstack, 1);
							if (!itemstackslottransaction.succeeded()) {
								context.getState().state = InteractionState.Failed;
								return;
							}

							frameComponent.setHeldStack(stackClone);
							commandBuffer.run(entityStore -> {
								FrameUtil.remakeItemEntity(entityStore, boundRef, stackClone, yawDegrees);
								world.performBlockUpdate(x, y, z);
							});
						}
					}
				} else {
					if (itemstack == null || itemstack.isEmpty()) {
						ItemHelper.spawnItem(commandBuffer, frameComponent, boundRef);
						frameComponent.setHeldStack(null);
						commandBuffer.run(entityStore -> {
							FrameUtil.remakeItemEntity(entityStore, boundRef, null, yawDegrees);
							world.performBlockUpdate(x, y, z);
						});
					}
				}
			}
		}
	}

	@Override
	protected void simulateInteractWithBlock(@Nonnull InteractionType type,
	                                         @Nonnull InteractionContext interactionContext,
	                                         @Nullable ItemStack itemStack, @Nonnull World world,
	                                         @Nonnull Vector3i targetBlock) {

	}
}
