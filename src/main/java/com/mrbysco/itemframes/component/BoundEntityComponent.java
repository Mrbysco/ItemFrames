package com.mrbysco.itemframes.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.mrbysco.itemframes.ItemFramePlugin;

import java.util.UUID;

public class BoundEntityComponent implements Component<ChunkStore> {
	public static final BuilderCodec CODEC;
	private UUID attachedEntity;

	public static ComponentType<ChunkStore, BoundEntityComponent> getComponentType() {
		return ItemFramePlugin.get().getBoundEntityComponent();
	}

	private BoundEntityComponent() {
	}

	public BoundEntityComponent(UUID attachedEntity) {
		this.attachedEntity = attachedEntity;
	}

	public void setAttachedEntity(UUID attachedEntity) {
		this.attachedEntity = attachedEntity;
	}

	public UUID getAttachedEntity() {
		return attachedEntity;
	}

	public Component<ChunkStore> clone() {
		return new BoundEntityComponent(this.attachedEntity);
	}

	static {
		CODEC = BuilderCodec.builder(BoundEntityComponent.class, BoundEntityComponent::new)
				.append(new KeyedCodec<>("BoundEntity", Codec.UUID_BINARY),
						(component, attachedEntity) -> component.attachedEntity = attachedEntity,
						(component) -> component.attachedEntity).add()
				.build();
	}
}
