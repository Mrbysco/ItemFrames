package com.mrbysco.itemframes.util;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.mrbysco.itemframes.component.ItemFrameComponent;

public class ItemHelper {
	public static void spawnItem(ComponentAccessor<EntityStore> store, ItemFrameComponent component, Ref<EntityStore> ref) {
		Vector3d position = null;
		if (component.getFramePosition() != null) {
			position = component.getFramePosition().toVector3d().add(0.5, 0.25, 0.5);
		}
		if (position == null) {
			TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
			if (transformComponent != null) {
				position = transformComponent.getPosition();
			} else {
				return;
			}
		}
		Holder<EntityStore> holder = ItemComponent.generateItemDrop(
				store,
				component.getHeldStack(),
				position,
				Vector3f.ZERO,
				0.0F,
				0.0F,
				0.0F
		);
		if (holder != null) {
			ItemComponent itemcomponent = holder.getComponent(ItemComponent.getComponentType());
			if (itemcomponent != null) {
				itemcomponent.setPickupDelay(1.5F);
			}

			store.addEntity(holder, AddReason.SPAWN);
		}
	}
}
