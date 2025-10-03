package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

/**
 * Migrator to rename the "use stone buttons" permission to "use buttons".
 * This was necessary to support all button types, not just stone buttons.
 */
public class MigratorMPerm003RenameStoneButtons extends MigratorRoot
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    private static MigratorMPerm003RenameStoneButtons i = new MigratorMPerm003RenameStoneButtons();
    public static MigratorMPerm003RenameStoneButtons get() { return i; }
    private MigratorMPerm003RenameStoneButtons()
    {
        super(MPerm.class);
    }

    // -------------------------------------------- //
    // OVERRIDE
    // -------------------------------------------- //

    @Override
    public void migrateInner(JsonObject entity)
    {
        JsonElement jsonDesc = entity.get("desc");
        String desc = jsonDesc.getAsString();
        if (desc.equalsIgnoreCase("use stone buttons")) desc = "use buttons";

        entity.addProperty("desc", desc);
    }

}
