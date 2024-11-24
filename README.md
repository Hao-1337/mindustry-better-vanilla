# Better Vanilla

![GitHub](https://img.shields.io/github/stars/Hao-1337/mindustry-better-vanilla?label=Stars)
![GitHub](https://img.shields.io/github/forks/Hao-1337/mindustry-better-vanilla)
![GitHub](https://img.shields.io/github/contributors/Hao-1337/mindustry-better-vanilla?label=Contributors)
![GitHub](https://img.shields.io/github/license/Hao-1337/mindustry-better-vanilla?label=License)
![GitHub](https://img.shields.io/github/issues/Hao-1337/mindustry-better-vanilla?label=Issues)
![GitHub](https://img.shields.io/github/commit-activity/m/Hao-1337/mindustry-better-vanilla?label=Commits)

Add enhancements and improvements to Mindustry's vanilla experience, including better UI and useful features for players.

### Textures and Sprites
Created by: [ĐạiPH](https://github.com/BackNNHH)

---

> **WARNING:**
> This mod is under active development. It may crash, have errors, or not work as intended. Please report any issues so they can be addressed quickly.

---

## Features

- **Core item display** for mobile.
- **Unit counter display**.
- **Time control**.
- Vanilla changes (see settings, default is off):
  - Bigger vault and container capacities (Erekir).
  - Increased range for override dome with 200% speed boost.
  - Force projector with higher shield health.
- **Auto-updates**.
- New content:
  - Thorium/Surge conveyors (Serpulo).
  - Additional storage blocks.
  - "T6" units.
- Unlocks some hidden content.

---

# Experimental Section

### Multi-block Machines

![Multi-block Machine](https://github.com/user-attachments/assets/d14f5326-0bb2-4662-b880-f4ee980e549e)

> **NOTE:**
> Only prototypes are available currently.

Example JSON configuration for a multi-block machine:

```json
{
  "name": "RBMK Build Projector",
  "type": "hao1337.lib.MultiBlockMachine",
  "category": "power",
  "hasPower": true,
  "size": 3,
  "selfIsMachine": true,
  "areaSize": 4,
  "build": [
    {
      "block": "hao1337-mod-rbmk-reactor-controller",
      "pos": [
        { "x": 0, "y": 0 }
      ]
    },
    {
      "block": "carbide-wall",
      "pos": [
        { "x": 3, "y": 3 }
      ]
    }
  ],
  "requirements": [
    "oxide/60",
    "silicon/120",
    "tungsten/80",
    "carbide/60"
  ]
}
```

---

# Screenshots

### Unit Counter and Core Item Display (Mobile)

![Preview](https://github.com/Hao-1337/mindustry-better-vanilla/assets/108588018/72654879-1a5b-4f70-a443-d9b362eb2136)

### Time Control

![Time Control](https://github.com/user-attachments/assets/bdd6a206-f10e-4bb1-8e9a-3388cb74d5b7)

### Leviathan Reconstructor

![Leviathan Reconstructor](https://github.com/user-attachments/assets/c54194e3-9136-45f5-86eb-ea750f8912ed)

