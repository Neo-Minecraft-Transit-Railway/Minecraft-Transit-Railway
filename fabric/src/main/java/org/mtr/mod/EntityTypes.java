package org.mtr.mod;

import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.registry.EntityTypeRegistryObject;
import org.mtr.mod.entity.EntityRendering;

public final class EntityTypes {

	static {
		// Non-zero dimensions so the entity stays in the client world / renderer path on 1.21+.
		RENDERING = Init.REGISTRY.registerEntityType(new Identifier(Init.MOD_ID, "rendering"), EntityRendering::new, 1F, 1F);
	}

	public static final EntityTypeRegistryObject<EntityRendering> RENDERING;

	public static void init() {
		Init.LOGGER.info("Registering Minecraft Transit Railway entity types");
	}
}
