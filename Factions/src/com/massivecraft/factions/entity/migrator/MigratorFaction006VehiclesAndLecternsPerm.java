package com.massivecraft.factions.entity.migrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.store.migrator.MigratorRoot;

public class MigratorFaction006VehiclesAndLecternsPerm extends MigratorRoot
{
    // -------------------------------------------- //
    // INSTANCE & CONSTRUCT
    // -------------------------------------------- //

    private static MigratorFaction006VehiclesAndLecternsPerm i = new MigratorFaction006VehiclesAndLecternsPerm();
    public static MigratorFaction006VehiclesAndLecternsPerm get() { return i; }
    private MigratorFaction006VehiclesAndLecternsPerm()
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

        // Vehicle perm (place/destroy vehicles): copy from door (only if not already present)
        JsonElement doorPerm = perms.has("door") ? perms.get("door") : null;
        if (doorPerm != null && !perms.has("vehicle"))
        {
            perms.add("vehicle", doorPerm.deepCopy());
        }

        // Lectern perm (take books from lecterns): copy from container (only if not already present)
        JsonElement containerPerm = perms.has("container") ? perms.get("container") : null;
        if (containerPerm != null && !perms.has("lectern"))
        {
            perms.add("lectern", containerPerm.deepCopy());
        }
    }
}