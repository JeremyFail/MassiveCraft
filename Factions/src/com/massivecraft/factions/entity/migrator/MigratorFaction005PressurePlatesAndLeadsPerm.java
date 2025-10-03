package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorFaction005PressurePlatesAndLeadsPerm extends MigratorRoot
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    private static MigratorFaction005PressurePlatesAndLeadsPerm i = new MigratorFaction005PressurePlatesAndLeadsPerm();
    public static MigratorFaction005PressurePlatesAndLeadsPerm get() { return i; }
    private MigratorFaction005PressurePlatesAndLeadsPerm()
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

        JsonElement permsToCopy = null;
        if (perms.has("button"))
        {
            permsToCopy = perms.get("button");
        }
        if (permsToCopy != null)
        {
            // Only add if not already present - Deep copy to avoid reference issues
            if (!perms.has("pressureplate"))
            {
                JsonElement pressureplatePerm = permsToCopy.deepCopy();
                perms.add("pressureplate", pressureplatePerm);
            }
            if (!perms.has("lead"))
            {
                JsonElement leadPerm = permsToCopy.deepCopy();
                perms.add("lead", leadPerm);
            }
        }
        
    }
}