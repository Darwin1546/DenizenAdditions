package org.darwin.denizenAdditions.tags;

public class Tags {

    public static MegModeledEntityTag getModeledEntity(EntityTag entity) {
        return new MegModeledEntityTag(entity.getBukkitEntity());
    }

    public static void register() {
        // <--[tag]
        // @attribute <EntityTag.modeled_entity>
        // @returns MegModeledEntityTag
        // @plugin Megizen
        // @description
        // Returns the modeled entity of the entity, if any.
        // -->
        EntityTag.tagProcessor.registerTag(MegModeledEntityTag.class, "modeled_entity", (attribute, entity) -> {
            return getModeledEntity(entity);
        });

        // <--[tag]
        // @attribute <EntityTag.mounted_bone>
        // @returns MegBoneTag
        // @plugin Megizen
        // @description
        // Returns the MegBoneTag that the entity is mounted on, if any.
        // -->
        EntityTag.tagProcessor.registerTag(MegBoneTag.class, "mounted_bone", (attribute, entity) -> {
            MountController controller = ModelEngineAPI.getMountPairManager().getController(entity.getUUID());
            if (controller == null || controller.getMount() == null) {
                return null;
            }
            return new MegBoneTag(((BoneBehavior) controller.getMount()).getBone());
        });
    }
}
