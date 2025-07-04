package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorFaction005PressurePlatesPerm extends MigratorRoot
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    private static MigratorFaction005PressurePlatesPerm i = new MigratorFaction005PressurePlatesPerm();
    public static MigratorFaction005PressurePlatesPerm get() { return i; }
    private MigratorFaction005PressurePlatesPerm()
    {
        super(Faction.class);
    }

    // -------------------------------------------- //
    // OVERRIDE
    // -------------------------------------------- //

    @Override
    public void migrateInner(JsonObject entity)
    {
        JsonObject perms = entity.getAsJsonObject("perms");
        if (perms == null) return;

        // Only add if not already present
        if (!perms.has("pressureplate") && perms.has("button")) {
            JsonElement buttonPerm = perms.get("button");
            // Deep copy to avoid reference issues
            JsonElement pressureplatePerm = buttonPerm.deepCopy();
            perms.add("pressureplate", pressureplatePerm);
        }
    }
}