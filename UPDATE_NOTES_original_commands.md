# MCOptimizer Commands

This document provides a comprehensive list of all available commands in MCOptimizer and explains how they interact with the configuration file (`config.yml`).

## Main Command

`/mcoptimizer` or `/mcopt` or `/mco` - The main command for the plugin.

## Admin Commands

### Help
**Command:** `/mcoptimizer help`  
**Permission:** `mcoptimizer.use`  
**Description:** Displays all available commands and their descriptions.

### Status
**Command:** `/mcoptimizer status [lag]`  
**Permission:** `mcoptimizer.status`  
**Description:** Shows current optimization status, memory usage, and performance metrics. With the `lag` subcommand, displays the lag prediction status.
**Config Relation:** Displays information from various sections in config.yml like memory usage, entity counts, etc. The lag prediction information uses settings from the `lag-prediction` section.

### Reload
**Command:** `/mcoptimizer reload`  
**Permission:** `mcoptimizer.reload`  
**Description:** Reloads the plugin configuration from disk.
**Config Relation:** Reloads all settings from config.yml and applies them immediately.

### Config
**Command:** `/mcoptimizer config [view|analyze|optimize] [file]`  
**Permission:** `mcoptimizer.config`  
**Description:** View, analyze, or optimize server configuration files.
**Config Relation:** Uses settings in the `config-analyzer` section to determine which files to check and what optimizations to suggest.

### Memory
**Command:** `/mcoptimizer memory [status|check|analyze]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Memory management and leak detection commands.
**Config Relation:** Uses settings in the `memory-leak` section to control detection thresholds and actions.
- `status`: Shows current memory usage statistics.
- `check`: Forces an immediate memory leak check.
- `analyze`: Analyzes potential memory leak sources.

### Entity
**Command:** `/mcoptimizer entity [status|optimize|count]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Entity management and optimization commands.
**Config Relation:** Uses settings in the `entity` section including farm-protection options.
- `status`: Shows current entity optimization status.
- `optimize`: Forces immediate entity optimization.
- `count`: Shows entity counts by type and world.

### Chunk
**Command:** `/mcoptimizer chunk [status|optimize|unload]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Chunk management and optimization commands.
**Config Relation:** Uses settings in the `chunk` section.
- `status`: Shows current chunk loading status.
- `optimize`: Forces immediate chunk optimization.
- `unload`: Unloads unused chunks to free memory.

### Redstone
**Command:** `/mcoptimizer redstone [status|optimize]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Redstone circuit optimization commands.
**Config Relation:** Uses settings in the `redstone` section.
- `status`: Shows current redstone optimization status.
- `optimize`: Optimizes redstone circuits for better performance.

### TNT
**Command:** `/mcoptimizer tnt [status|enable|disable]`  
**Permission:** `mcoptimizer.admin`  
**Description:** TNT explosion optimization commands.
**Config Relation:** Uses settings in the `tnt-optimization` section.
- `status`: Shows current TNT optimization status.
- `enable`: Enables TNT optimization.
- `disable`: Disables TNT optimization.

### Web
**Command:** `/mcoptimizer web [status|start|stop|credentials]`  
**Permission:** `mcoptimizer.web`  
**Description:** Manage the web panel interface.
**Config Relation:** Uses settings in the `web-panel` section.
- `status`: Shows current web panel status.
- `start`: Starts the web panel if it's disabled.
- `stop`: Stops the web panel.
- `credentials`: Displays or resets login credentials.

### Report
**Command:** `/mcoptimizer report [generate|view|list]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Performance report management commands.
**Config Relation:** Uses settings in the `performance-reports` section.
- `generate`: Generates a new performance report.
- `view`: Views the latest or specific performance report.
- `list`: Lists all available performance reports.

### Debug
**Command:** `/mcoptimizer debug [on|off|log]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Debug mode control and log viewing.
**Config Relation:** Relates to the `general.debug` setting in config.yml.
- `on`: Enables debug logging.
- `off`: Disables debug logging.
- `log`: Views recent debug logs.

### Farm
**Command:** `/mcoptimizer farm [protect|status] [add|remove] [radius]`  
**Permission:** `mcoptimizer.admin`  
**Description:** Farm protection management to ensure farms are not affected by optimizations.
**Config Relation:** Uses settings in the `entity.farm-protection` section.
- `protect add`: Adds farm protection at the current location with specified radius.
- `protect remove`: Removes farm protection from current location.
- `status`: Shows all protected farm areas.

### Version
**Command:** `/mcoptimizer version`  
**Permission:** `mcoptimizer.use`  
**Description:** Shows plugin version information and credits.

## Command Organization

The commands follow a hierarchical structure:
```
/mcoptimizer (main command)
  ├── help - Shows command help
  ├── status - Shows overall status
  │     └── lag - Shows lag prediction status
  ├── reload - Reloads configuration
  ├── config - Configuration management
  │     ├── view [file] - View a configuration file
  │     ├── analyze [file] - Analyze a configuration file
  │     └── optimize [file] - Suggest optimizations
  ├── memory - Memory management
  │     ├── status - Memory stats
  │     ├── check - Run leak detection
  │     └── analyze - Analyze leak sources
  ├── entity - Entity optimization
  │     ├── status - Entity stats
  │     ├── optimize - Run entity optimization
  │     └── count - Count entities by type
  └── (other subcommands)
```

## Example Usage

1. Check server optimization status:
   ```
   /mcoptimizer status
   ```

2. Optimize entity processing to free up server resources:
   ```
   /mcoptimizer entity optimize
   ```

3. Protect a farm area from optimization at your current location:
   ```
   /mcoptimizer farm protect add 16
   ```

4. Generate a performance report:
   ```
   /mcoptimizer report generate
   ```

5. Check memory usage and potential leaks:
   ```
   /mcoptimizer memory check
   ```

6. Check lag prediction status:
   ```
   /mcoptimizer status lag
   ```

## Config Integration

All commands interact with the configuration file in some way. When you modify the config.yml file, the commands will respect those changes immediately after using the `/mcoptimizer reload` command.

For example, if you modify `entity.farm-protection.farm-detection-radius` in config.yml, the farm protection command will use that new radius as the default.

## Credits

Plugin by juicyyfruittsnackss (https://dsc.gg/fruitsnacks)