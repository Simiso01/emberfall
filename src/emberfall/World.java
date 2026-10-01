package emberfall;

import java.util.Random;

/**
 * Cellular world. Each cell packs a material id in the low byte and a data
 * byte (color variation or lifetime) above it.
 */
final class World {
    final int width;
    final int height;
    private final int[] cells;
    private final boolean[] updated;
    private final Random random;

    World(int width, int height, Random random) {
        this.width = width;
        this.height = height;
        this.cells = new int[width * height];
        this.updated = new boolean[width * height];
        this.random = random;
    }

    void clear() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 0;
        }
    }

    Material materialAt(int x, int y) {
        if (!inside(x, y)) {
            return Material.EMPTY;
        }
        return Material.values()[materialId(cells[index(x, y)])];
    }

    int count(Material material) {
        int id = material.ordinal();
        int total = 0;
        for (int cell : cells) {
            if (materialId(cell) == id) {
                total++;
            }
        }
        return total;
    }

    void paint(int x, int y, int radius, Material material) {
        int r2 = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy > r2) {
                    continue;
                }
                set(x + dx, y + dy, material, freshData(material));
            }
        }
    }

    void loadVolcano() {
        clear();
        for (int x = 8; x < width - 8; x++) {
            set(x, height - 2, Material.STONE, 0);
            set(x, height - 1, Material.STONE, 0);
        }
        for (int y = height / 2; y < height - 2; y++) {
            set(8, y, Material.STONE, 0);
            set(width - 9, y, Material.STONE, 0);
        }
        int bowlLeft = width / 2 - 28;
        int bowlRight = width / 2 + 28;
        for (int y = height / 2; y < height - 2; y++) {
            set(bowlLeft, y, Material.STONE, 0);
            set(bowlRight, y, Material.STONE, 0);
        }
        for (int x = bowlLeft; x <= bowlRight; x++) {
            set(x, height / 2, Material.STONE, 0);
        }
        for (int y = height / 2 + 4; y < height - 6; y++) {
            for (int x = bowlLeft + 2; x < bowlRight - 1; x++) {
                set(x, y, Material.LAVA, 40);
            }
        }
        for (int y = height / 2 - 18; y < height / 2; y++) {
            for (int x = 20; x < bowlLeft - 2; x++) {
                if (y > height / 2 - 6 || random.nextFloat() < 0.55f) {
                    set(x, y, Material.SAND, freshData(Material.SAND));
                }
            }
        }
        for (int x = bowlRight + 6; x < width - 16; x++) {
            set(x, height - 8, Material.WATER, 0);
            set(x, height - 7, Material.WATER, 0);
            set(x, height - 6, Material.WATER, 0);
        }
        for (int x = 24; x < 70; x++) {
            set(x, height - 10, Material.GUNPOWDER, 0);
        }
        set(22, height - 10, Material.FIRE, 30);
        for (int y = height - 22; y < height - 10; y++) {
            set(width - 24, y, Material.WOOD, 0);
            set(width - 20, y, Material.WOOD, 0);
        }
        for (int x = width - 28; x < width - 16; x++) {
            set(x, height - 22, Material.WOOD, 0);
        }
        for (int y = height - 16; y < height - 8; y++) {
            set(width - 40, y, Material.PLANT, 0);
        }
        set(width - 40, height - 8, Material.WATER, 0);
        for (int y = height / 2; y < height / 2 + 16; y++) {
            set(16, y, Material.ACID, 0);
        }
        for (int x = 30; x < 48; x++) {
            set(x, 12, Material.ICE, 0);
        }
    }

    void step() {
        java.util.Arrays.fill(updated, false);
        boolean leftToRight = random.nextBoolean();
        for (int y = height - 1; y >= 0; y--) {
            if (leftToRight) {
                for (int x = 0; x < width; x++) {
                    update(x, y);
                }
            } else {
                for (int x = width - 1; x >= 0; x--) {
                    update(x, y);
                }
            }
        }
    }

    void render(int[] pixels) {
        Material[] values = Material.values();
        for (int i = 0; i < cells.length; i++) {
            int cell = cells[i];
            int id = materialId(cell);
            int color = values[id].color;
            int data = data(cell);
            if (id == Material.SAND.ordinal() || id == Material.STONE.ordinal()) {
                int shift = (data % 18) - 9;
                color = shiftChannel(color, shift);
            } else if (id == Material.FIRE.ordinal() || id == Material.LAVA.ordinal()) {
                color = shiftChannel(color, (data % 24) - 6);
            } else if (id == Material.WATER.ordinal()) {
                color = shiftChannel(color, (i % 7) - 3);
            }
            pixels[i] = color;
        }
    }

    private void update(int x, int y) {
        int i = index(x, y);
        if (updated[i]) {
            return;
        }
        int id = materialId(cells[i]);
        if (id == Material.EMPTY.ordinal() || id == Material.STONE.ordinal() || id == Material.GLASS.ordinal()) {
            return;
        }
        Material material = Material.values()[id];
        switch (material) {
            case SAND, SALT, GUNPOWDER -> fallLikePowder(x, y, material);
            case WATER -> flow(x, y, Material.WATER, false);
            case OIL -> flow(x, y, Material.OIL, true);
            case ACID -> flowAcid(x, y);
            case LAVA -> flowLava(x, y);
            case FIRE -> burn(x, y);
            case SMOKE, STEAM -> rise(x, y, material);
            case PLANT -> grow(x, y);
            case WOOD -> catchFire(x, y);
            case ICE -> meltOrFreeze(x, y);
            default -> {
            }
        }
    }

    private void fallLikePowder(int x, int y, Material material) {
        if (material == Material.GUNPOWDER && neighborIs(x, y, Material.FIRE)) {
            explode(x, y);
            return;
        }
        if (material == Material.SALT && neighborIs(x, y, Material.WATER)) {
            set(x, y, Material.EMPTY, 0);
            return;
        }
        if (fall(x, y)) {
            return;
        }
        int below = materialId(cell(x, y + 1));
        if (material == Material.SAND && (below == Material.WATER.ordinal() || below == Material.OIL.ordinal())) {
            swap(x, y, x, y + 1);
            return;
        }
        int dir = random.nextBoolean() ? -1 : 1;
        if (isEmpty(x + dir, y + 1)) {
            move(x, y, x + dir, y + 1);
        } else if (isEmpty(x - dir, y + 1)) {
            move(x, y, x - dir, y + 1);
        }
    }

    private void flow(int x, int y, Material self, boolean floatsOnWater) {
        if (self == Material.OIL && neighborIs(x, y, Material.FIRE)) {
            set(x, y, Material.FIRE, 18 + random.nextInt(20));
            return;
        }
        if (self == Material.WATER && neighborIs(x, y, Material.LAVA)) {
            set(x, y, Material.STEAM, 24);
            return;
        }
        if (self == Material.WATER && neighborIs(x, y, Material.FIRE)) {
            set(x, y, Material.STEAM, 16);
            return;
        }
        if (floatsOnWater && materialId(cell(x, y + 1)) == Material.WATER.ordinal()) {
            swap(x, y, x, y + 1);
            return;
        }
        if (fall(x, y)) {
            return;
        }
        int dir = random.nextBoolean() ? -1 : 1;
        if (isEmpty(x + dir, y + 1)) {
            move(x, y, x + dir, y + 1);
            return;
        }
        if (isEmpty(x - dir, y + 1)) {
            move(x, y, x - dir, y + 1);
            return;
        }
        int reach = self == Material.OIL ? 2 : 4;
        for (int step = 1; step <= reach; step++) {
            if (!isEmpty(x + dir * step, y)) {
                break;
            }
            if (step == reach || !isEmpty(x + dir * (step + 1), y) || random.nextBoolean()) {
                move(x, y, x + dir * step, y);
                return;
            }
        }
    }

    private void flowAcid(int x, int y) {
        dissolveAround(x, y);
        if (updated[index(x, y)] || materialId(cells[index(x, y)]) != Material.ACID.ordinal()) {
            return;
        }
        flow(x, y, Material.ACID, false);
    }

    private void dissolveAround(int x, int y) {
        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : neighbors) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (!inside(nx, ny)) {
                continue;
            }
            Material other = Material.values()[materialId(cell(nx, ny))];
            if (other.dissolvable && random.nextFloat() < 0.35f) {
                set(nx, ny, Material.EMPTY, 0);
                if (random.nextFloat() < 0.25f) {
                    set(x, y, Material.EMPTY, 0);
                    return;
                }
            }
        }
    }

    private void flowLava(int x, int y) {
        bakeAndIgnite(x, y);
        if (materialId(cells[index(x, y)]) != Material.LAVA.ordinal()) {
            return;
        }
        if (random.nextFloat() > 0.55f) {
            return;
        }
        int below = materialId(cell(x, y + 1));
        if (below == Material.WATER.ordinal()) {
            set(x, y + 1, Material.STEAM, 28);
            set(x, y, Material.STONE, freshData(Material.STONE));
            return;
        }
        fallLikePowder(x, y, Material.LAVA);
    }

    private void bakeAndIgnite(int x, int y) {
        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : neighbors) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (!inside(nx, ny)) {
                continue;
            }
            int id = materialId(cell(nx, ny));
            if (id == Material.WATER.ordinal()) {
                set(nx, ny, Material.STEAM, 26);
                set(x, y, Material.STONE, freshData(Material.STONE));
                return;
            }
            if (id == Material.SAND.ordinal() && random.nextFloat() < 0.12f) {
                set(nx, ny, Material.GLASS, 0);
            } else if (id == Material.ICE.ordinal()) {
                set(nx, ny, Material.WATER, 0);
            } else if (Material.values()[id].flammable && random.nextFloat() < 0.4f) {
                ignite(nx, ny);
            }
        }
    }

    private void burn(int x, int y) {
        int life = data(cells[index(x, y)]) - 1;
        if (life <= 0) {
            if (random.nextFloat() < 0.65f) {
                set(x, y, Material.SMOKE, 10 + random.nextInt(16));
            } else {
                set(x, y, Material.EMPTY, 0);
            }
            return;
        }
        cells[index(x, y)] = pack(Material.FIRE, life);
        igniteNeighbors(x, y);
        int dir = random.nextInt(3) - 1;
        if (isEmpty(x + dir, y - 1)) {
            move(x, y, x + dir, y - 1);
        } else if (random.nextFloat() < 0.2f) {
            set(x, y, Material.SMOKE, 8 + random.nextInt(10));
        }
    }

    private void igniteNeighbors(int x, int y) {
        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, -1}, {-1, -1}};
        for (int[] offset : neighbors) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (!inside(nx, ny)) {
                continue;
            }
            int id = materialId(cell(nx, ny));
            if (id == Material.GUNPOWDER.ordinal()) {
                explode(nx, ny);
                return;
            }
            if (Material.values()[id].flammable && random.nextFloat() < 0.35f) {
                ignite(nx, ny);
            }
        }
    }

    private void ignite(int x, int y) {
        int id = materialId(cell(x, y));
        if (id == Material.GUNPOWDER.ordinal()) {
            explode(x, y);
            return;
        }
        set(x, y, Material.FIRE, 16 + random.nextInt(28));
    }

    private void explode(int x, int y) {
        int radius = 5;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy > radius * radius) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (!inside(nx, ny)) {
                    continue;
                }
                int id = materialId(cell(nx, ny));
                if (id == Material.STONE.ordinal() || id == Material.GLASS.ordinal()) {
                    continue;
                }
                set(nx, ny, Material.FIRE, 12 + random.nextInt(20));
            }
        }
    }

    private void rise(int x, int y, Material material) {
        int life = data(cells[index(x, y)]) - 1;
        if (life <= 0) {
            if (material == Material.STEAM && random.nextFloat() < 0.45f) {
                set(x, y, Material.WATER, 0);
            } else {
                set(x, y, Material.EMPTY, 0);
            }
            return;
        }
        cells[index(x, y)] = pack(material, life);
        int dir = random.nextInt(3) - 1;
        if (isEmpty(x + dir, y - 1)) {
            move(x, y, x + dir, y - 1);
        } else if (isEmpty(x, y - 1)) {
            move(x, y, x, y - 1);
        }
    }

    private void grow(int x, int y) {
        if (neighborIs(x, y, Material.FIRE) || neighborIs(x, y, Material.LAVA)) {
            set(x, y, Material.FIRE, 20);
            return;
        }
        if (random.nextFloat() < 0.04f && isEmpty(x, y - 1) && neighborIs(x, y, Material.WATER)) {
            set(x, y - 1, Material.PLANT, 0);
        }
    }

    private void catchFire(int x, int y) {
        if (neighborIs(x, y, Material.FIRE) || neighborIs(x, y, Material.LAVA)) {
            if (random.nextFloat() < 0.25f) {
                set(x, y, Material.FIRE, 30 + random.nextInt(20));
            }
        }
    }

    private void meltOrFreeze(int x, int y) {
        if (neighborIs(x, y, Material.FIRE) || neighborIs(x, y, Material.LAVA)) {
            set(x, y, Material.WATER, 0);
            return;
        }
        if (random.nextFloat() > 0.03f) {
            return;
        }
        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : neighbors) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (inside(nx, ny) && materialId(cell(nx, ny)) == Material.WATER.ordinal()) {
                set(nx, ny, Material.ICE, 0);
                return;
            }
        }
    }

    private boolean fall(int x, int y) {
        if (!isEmpty(x, y + 1)) {
            return false;
        }
        move(x, y, x, y + 1);
        return true;
    }

    private void move(int x, int y, int nx, int ny) {
        int from = index(x, y);
        int to = index(nx, ny);
        cells[to] = cells[from];
        cells[from] = 0;
        updated[from] = true;
        updated[to] = true;
    }

    private void swap(int x, int y, int nx, int ny) {
        int a = index(x, y);
        int b = index(nx, ny);
        int tmp = cells[a];
        cells[a] = cells[b];
        cells[b] = tmp;
        updated[a] = true;
        updated[b] = true;
    }

    private void set(int x, int y, Material material, int data) {
        if (!inside(x, y)) {
            return;
        }
        int i = index(x, y);
        cells[i] = pack(material, data);
        updated[i] = true;
    }

    private boolean neighborIs(int x, int y, Material material) {
        return materialId(cell(x + 1, y)) == material.ordinal()
                || materialId(cell(x - 1, y)) == material.ordinal()
                || materialId(cell(x, y + 1)) == material.ordinal()
                || materialId(cell(x, y - 1)) == material.ordinal();
    }

    private boolean isEmpty(int x, int y) {
        return inside(x, y) && materialId(cells[index(x, y)]) == Material.EMPTY.ordinal();
    }

    private int cell(int x, int y) {
        if (!inside(x, y)) {
            return pack(Material.STONE, 0);
        }
        return cells[index(x, y)];
    }

    private int freshData(Material material) {
        if (material == Material.SAND || material == Material.STONE) {
            return random.nextInt(24);
        }
        if (material == Material.LAVA) {
            return 30 + random.nextInt(20);
        }
        if (material == Material.FIRE) {
            return 20 + random.nextInt(20);
        }
        return 0;
    }

    private static int pack(Material material, int data) {
        return material.ordinal() | ((data & 0xFF) << 8);
    }

    private static int materialId(int cell) {
        return cell & 0xFF;
    }

    private static int data(int cell) {
        return (cell >> 8) & 0xFF;
    }

    private static int shiftChannel(int color, int amount) {
        int r = clamp(((color >> 16) & 0xFF) + amount);
        int g = clamp(((color >> 8) & 0xFF) + amount);
        int b = clamp((color & 0xFF) + amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    private boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private int index(int x, int y) {
        return y * width + x;
    }
}
