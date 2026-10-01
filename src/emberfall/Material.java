package emberfall;

enum Material {
    EMPTY("Erase", 0x141210, false, false),
    STONE("Stone", 0x6B675F, false, true),
    SAND("Sand", 0xC6A15A, false, true),
    WATER("Water", 0x3C7CC8, false, false),
    SALT("Salt", 0xE6E2D8, false, true),
    OIL("Oil", 0x3E3424, true, false),
    LAVA("Lava", 0xFF4A12, false, false),
    FIRE("Fire", 0xFF6A1A, false, false),
    SMOKE("Smoke", 0x8E8982, false, false),
    STEAM("Steam", 0xD7E6EF, false, false),
    WOOD("Wood", 0x6E4324, true, true),
    PLANT("Plant", 0x3E8E46, true, true),
    ACID("Acid", 0xB6E04A, false, false),
    GLASS("Glass", 0xB7D5DE, false, false),
    GUNPOWDER("Powder", 0x4C4A44, true, false),
    ICE("Ice", 0xC5E8F4, false, false);

    final String label;
    final int color;
    final boolean flammable;
    final boolean dissolvable;

    Material(String label, int color, boolean flammable, boolean dissolvable) {
        this.label = label;
        this.color = color;
        this.flammable = flammable;
        this.dissolvable = dissolvable;
    }
}
