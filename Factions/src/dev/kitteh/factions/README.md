# Legacy Factions API Compatibility Layer

## ⚠️ DEPRECATED - DO NOT USE FOR NEW IMPLEMENTATIONS

This directory contains a **compatibility layer** for plugins that were originally developed for FactionsUUID and use the `dev.kitteh.factions` package namespace.

## Purpose

This compatibility layer exists solely to support existing plugins that:
- Were built against FactionsUUID that use the `dev.kitteh.factions` package
- Need to continue functioning until proper Factions3 support is added

## What This Is

- **Wrapper classes** that delegate to the modern Factions implementation
- **Bridge interfaces** that translate between old and new API calls
- **Compatibility methods** that maintain backward compatibility for legacy plugins

## What This Is NOT

- ❌ A full-featured API for new development
- ❌ A maintained or actively developed API
- ❌ A recommended approach for modern plugins
- ❌ A complete implementation of all legacy features

## Implementation Status

This compatibility layer provides:
- ✅ Basic faction operations (get/set properties)
- ✅ Player faction membership
- ✅ Territory and location queries
- ✅ Warp management
- ⚠️ **Partial implementation** of some advanced features
- ❌ **Missing implementation** for some legacy methods

## For Plugin Developers

### If you're developing a NEW plugin:
**Use the modern API**: You can read the developer documentation [in the Wiki](https://github.com/JeremyFail/MassiveCraft/wiki/%E2%80%90-Factions-Development-Guide-%E2%80%90).

### If you're maintaining an EXISTING plugin:
1. **Preferred**: Update your plugin to use the modern API.
2. **Temporary**: This compatibility layer may help during migration. Understand that it's deprecated, is not supported, may not fully work, and may be removed at a later date. If you experience issues, we will not be able to provide assistance.

## Warnings and Limitations

- **Performance**: Additional overhead due to wrapper delegation
- **Features**: Not all legacy methods are implemented
- **Maintenance**: This layer receives minimal maintenance
- **Compatibility**: May break with future Factions updates
- **Support**: Limited support for issues with legacy API usage

## Usage Monitoring

This compatibility layer includes usage tracking and will log warnings when legacy API methods are called. This helps server administrators identify plugins that need updating.

## Migration Guide

To migrate from the legacy API to the modern API:

1. Update method calls to use the modern API equivalents
2. Review faction entity structure changes
3. Test thoroughly with the new API

## Files in This Directory

- `Faction.java` - Legacy faction interface
- `FPlayer.java` - Legacy player interface  
- `FLocation.java` - Legacy location interface
- `Board.java` - Legacy board interface
- `FPlayers.java` - Legacy players manager interface
- `LegacyFaction.java` - Faction wrapper implementation
- `LegacyFPlayer.java` - FPlayer wrapper implementation

## Legal Notice

This compatibility layer is provided "as-is" without warranty. Use at your own risk for existing plugin compatibility only.

---

**For the best experience and full feature support, please migrate to the modern Factions API**
