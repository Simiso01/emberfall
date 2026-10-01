# Emberfall

A real-time falling-sand simulation written in Java. Materials react with each other: sand sinks, water flows, lava makes glass, acid eats stone, and gunpowder detonates.

The world is a grid of cells updated every frame. There is no game engine and no extra library. Rendering is a pixel buffer drawn with Swing.

## Run

```bat
javac -d out src\emberfall\*.java
java -cp out emberfall.EmberfallApp
```

Or double-click `run.bat`.

Check the physics without opening a window:

```bat
java -cp out emberfall.SimCheck
```

## Controls

| Action | Control |
|---|---|
| Paint | Left drag |
| Erase | Right drag |
| Choose material | Click a swatch, or press 1–9, 0, Q, W, E, R |
| Brush size | `[` and `]` |
| Pause | Space |
| Clear | C |
| Load the volcano | V |

## Materials

Stone, sand, water, salt, oil, lava, fire, smoke, steam, wood, plant, acid, glass, gunpowder, and ice.

Notable reactions:

- Sand falls and sinks through water and oil
- Lava turns water into steam and itself into stone
- Lava bakes nearby sand into glass
- Fire burns wood, plants, and oil, and sets off gunpowder
- Acid dissolves sand, stone, wood, and plants
- Plants creep upward when water is beside them
- Ice melts near fire or lava, and can freeze nearby water
