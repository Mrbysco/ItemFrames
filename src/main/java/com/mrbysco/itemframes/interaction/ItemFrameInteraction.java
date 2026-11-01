package com.mrbysco.itemframes.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.ItemFramePlugin;
import com.mrbysco.itemframes.component.BoundEntityComponent;
import com.mrbysco.itemframes.component.ItemFrameComponent;
import com.mrbysco.itemframes.util.FrameUtil;
import com.mrbysco.itemframes.util.ItemHelper;
import org.joml.Vector3i;

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

		int x = targetBlock.x();
		int y = targetBlock.y();
		int z = targetBlock.z();

		ChunkStore chunkStore = world.getChunkStore();
		Ref<ChunkStore> sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
		if (sectionRef != null && sectionRef.isValid()) {
			BlockSection blockSection = chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
			if (blockSection != null) {
				int blockId = blockSection.get(x, y, z);
				BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
				if (blockType == null) {
					context.getState().state = InteractionState.Failed;
					return;
				}
				Store<ChunkStore> chunkComponentStore = world.getChunkStore().getStore();

				BoundEntityComponent boundEntityComponent = chunkComponentStore.getComponent(sectionRef, ItemFramePlugin.get().getBoundEntityComponent());
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
						ItemStack heldStack = frameComponent.getHeldStack();

						if (heldStack != null && !heldStack.isEmpty()) {
							ItemHelper.spawnItem(commandBuffer, frameComponent, boundRef);
							frameComponent.setHeldStack(null);
						}

						heldStack = frameComponent.getHeldStack();
						// Clone the held stack for comparison later
						ItemStack heldClone = heldStack != null ? heldStack.withQuantity(1) : null;
						if (heldStack == null) {
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
								}
							}
						}

						if (heldClone != frameComponent.getHeldStack()) {
							commandBuffer.run(entityStore -> {
								BlockSection blockSection2 = sectionRef.getStore().getComponent(sectionRef, BlockSection.getComponentType());
								ChunkSection chunkSection2 = sectionRef.getStore().getComponent(sectionRef, ChunkSection.getComponentType());
								if (blockSection2 != null && chunkSection2 != null && boundRef.isValid()) {
									FrameUtil.remakeItemEntity(entityStore, boundRef, frameComponent.getHeldStack(), yawDegrees);
									BlockOperations.updateBlockArea(world.getChunkStore(), chunkSection2, blockSection2, x, y, z);
								}
							});
						}
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
