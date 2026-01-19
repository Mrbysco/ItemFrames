package com.mrbysco.itemframes.util;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.AssetIconProperties;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class FrameUtil {
	private static final List<String> itemFrameItems = List.of(
			"ItemFrames_Item_Frame",
			"ItemFrames_Item_Frame_Ancient",
			"ItemFrames_Item_Frame_Bamboo",
			"ItemFrames_Item_Frame_Kweebec",
			"ItemFrames_Item_Frame_Light",
			"ItemFrames_Item_Frame_Lumberjack",
			"ItemFrames_Item_Frame_Tavern"
	);

	public static boolean isItemFrame(String frameId) {
		return itemFrameItems.contains(frameId);
	}

	public static Ref<EntityStore> remakeItemEntity(
			@Nonnull Store<EntityStore> store,
			@Nonnull Ref<EntityStore> oldRef,
			@Nullable ItemStack stack
	) {
		// Remove existing variant-specific components
		removeVariantComponents(store, oldRef);

		// Copy the existing entity (includes transform, network, uuid, flags, etc.)
		Holder<EntityStore> holder = store.copyEntity(oldRef);

		float scale = 0.5F;

		if (stack != null) {
			Item item = stack.getItem();
			AssetIconProperties properties = item.getIconProperties();
			if (properties != null) {
				scale = properties.getScale();
			}

			stack.setOverrideDroppedItemAnimation(true);
			holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(stack));

			Model model = getItemModel(item);
			if (model != null) {
				String modelId = getItemModelId(item);
				if (modelId != null) {
					holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
					holder.addComponent(
							PersistentModel.getComponentType(),
							new PersistentModel(new Model.ModelReference(modelId, scale, null, true))
					);
				}
			} else if (item.hasBlockType()) {
				holder.addComponent(BlockEntity.getComponentType(), new BlockEntity(stack.getItemId()));
				holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale * 2.0F));
			} else {
				holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
			}
		}

		// Remove old entity AFTER copying
		store.removeEntity(oldRef, RemoveReason.REMOVE);

		// Respawn cleanly
		return store.addEntity(holder, AddReason.SPAWN);
	}

	public static void removeVariantComponents(Store<EntityStore> store, Ref<EntityStore> ref) {
		// variant-specific visuals/identity
		store.removeComponentIfExists(ref, ModelComponent.getComponentType());
		store.removeComponentIfExists(ref, PersistentModel.getComponentType());
		store.removeComponentIfExists(ref, BlockEntity.getComponentType());
		// scaling / network identity
		store.removeComponentIfExists(ref, EntityScaleComponent.getComponentType());

		// Remove item component last
		store.removeComponentIfExists(ref, ItemComponent.getComponentType());
	}

	@Nullable
	public static String getItemModelId(@Nonnull Item item) {
		String s = item.getModel();
		if (s == null && item.hasBlockType()) {
			BlockType blocktype = BlockType.getAssetMap().getAsset(item.getId());
			if (blocktype != null && blocktype.getCustomModel() != null) {
				s = blocktype.getCustomModel();
			}
		}

		return s;
	}

	@Nullable
	public static Model getItemModel(@Nonnull Item item) {
		String s = getItemModelId(item);
		if (s == null) {
			return null;
		} else {
			ModelAsset modelasset = ModelAsset.getAssetMap().getAsset(s);
			return modelasset != null ? Model.createStaticScaledModel(modelasset, 0.5F) : null;
		}
	}
}
