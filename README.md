# Better Vanilla

![GitHub](https://img.shields.io/github/stars/Hao-1337/mindustry-better-vanilla?label=Stars)
![GitHub](https://img.shields.io/github/forks/Hao-1337/mindustry-better-vanilla)
![GitHub](https://img.shields.io/github/contributors/Hao-1337/mindustry-better-vanilla?label=Contributors)
![GitHub](https://img.shields.io/github/license/Hao-1337/mindustry-better-vanilla?label=License)
![GitHub](https://img.shields.io/github/issues/Hao-1337/mindustry-better-vanilla?label=Issues)
![GitHub](https://img.shields.io/github/commit-activity/m/Hao-1337/mindustry-better-vanilla?label=Commits)

Add enhancements and improvements to Mindustry's vanilla experience, including better UI and useful features for players.

> [!WARNING]
> **This mod is under active development. It may crash, have errors, or not work as intended. Please report any issues so they can be addressed quickly.**

### Textures and Sprites
Created by: [ĐạiPH](https://github.com/BackNNHH)

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


# Screenshots

### Core items display / Units display
<img width="1600" height="900" alt="s7" src="https://github.com/user-attachments/assets/b4ad2b7e-aa8d-4e80-8dea-c983450d03a4" />

### Core Link
<img width="1600" height="900" alt="s2" src="https://github.com/user-attachments/assets/32b75a2c-7828-481d-9727-4c231bbfbba9" />

### Leviathan Recontructor
<img width="1600" height="900" alt="s2" src="https://github.com/user-attachments/assets/8edecd5e-771f-4bef-ab83-4f58337d9b4c" />

### Time control on single player
<img width="1600" height="900" alt="s5" src="https://github.com/user-attachments/assets/28757101-10c5-4579-a8b4-d7e1f40889b0" />

### Time control on multi player
<img width="1600" height="900" alt="s4" src="https://github.com/user-attachments/assets/7035d38c-15f8-4650-b779-211d88989314" />

### Turret: M1014
<img width="1600" height="900" alt="s6" src="https://github.com/user-attachments/assets/3cd612c5-2484-4d6f-8ab8-0bef1d91ca06" />

### How things look like on mobile:
![Screenshot_20260223_004608_Mindustry](https://github.com/user-attachments/assets/6ccaa84f-b426-4c5a-a79e-f758703d94bc)


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
