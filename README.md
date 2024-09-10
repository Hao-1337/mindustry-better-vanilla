<h1> [ Better Vanilla ] </h1>

![GitHub](https://img.shields.io/github/stars/Hao-1337/mindustry-better-vanilla?label=Stars)
![GitHub](https://img.shields.io/github/forks/Hao-1337/mindustry-better-vanilla)
![GitHub](https://img.shields.io/github/contributors/Hao-1337/mindustry-better-vanilla?label=Contributors)
![GitHub](https://img.shields.io/github/license/Hao-1337/mindustry-better-vanilla?label=License)
![GitHub](https://img.shields.io/github/issues/Hao-1337/mindustry-better-vanilla?label=Issues)
![GitHub](https://img.shields.io/github/commit-activity/m/Hao-1337/mindustry-better-vanilla?label=Commits)
<p>Add something helpful for vanilla. Also have some better UI.</p>

Textures and sprites made by: [ĐạiPH](https://github.com/BackNNHH)
<br>
<br>

> [!WARNING]
> The mod under development, it can be crash, error or not working.<br>
> Please report the issue for fix as fast as possible.

<h3>Features</h3>

- Coreitem display for mobile.
- Unit conter display.
- Time control.
- Some change for vanilla (see in setting, default is off):
  + Bigger vault, container and vault in Erekir.
  + Bigger range for override dome, 200% speed up.
  + Force projector have more shield health.
- Auto update.
- New contents as well:
  + Thorium / surge conveyor (serpulo)
  + Some storage blocks
  + "T6" units
- Aslo make some hidden contents available

<h1> Experimental Section </h1>
<h3> Multi block machine </h3>

![image](https://github.com/user-attachments/assets/d14f5326-0bb2-4662-b880-f4ee980e549e)

> [!NOTE]
> It just have some prototype for now.

- Here is example how you can do it in json:
```json
{
  "name": "RBMK Build Projector",
  "type": "hao1337.lib.MultiBlockMachine,",
  "category": "power,",
  "hasPower": true,
  "size": 3,
  "selfIsMachine": true,
  "areaSize": 4,
  "build": [
    {
      "block": "hao1337-mod-rbmk-reactor-controller",
      "pos": [
        {
          "x": 0,
          "y": 0
        }
      ]
    },
    {
      "block": "carbide-wall",
      "pos": [
        {
          "x": 3,
          "y": 3
        }
      ]
    }
  ],
  "requirements": [
    "oxide/60,",
    "silicon/120,",
    "tungsten/80,",
    "carbide/60"
  ]
}
```


<h1>Screenshots</h1>


<h4>Unit counter and coreitem(for mobile)</h4>

![Preview](https://github.com/Hao-1337/mindustry-better-vanilla/assets/108588018/72654879-1a5b-4f70-a443-d9b362eb2136)

<h4>Time control</h4>

![Screenshot 2024-08-03 185953](https://github.com/user-attachments/assets/bdd6a206-f10e-4bb1-8e9a-3388cb74d5b7)

<h4>Leviathan reconstructor</h4>

![Screenshot 2024-08-03 185904](https://github.com/user-attachments/assets/c54194e3-9136-45f5-86eb-ea750f8912ed)

