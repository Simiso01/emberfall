package emberfall;

import java.util.Random;

/** Headless checks for the reactions that make the simulation interesting. */
public final class SimCheck {
    public static void main(String[] args) {
        sandFalls();
        sandSinksThroughWater();
        waterSpreads();
        lavaMeetsWater();
        fireBurnsWood();
        gunpowderDetonates();
        acidEatsStone();
        System.out.println("All Emberfall checks passed.");
    }

    private static void sandFalls() {
        World world = new World(20, 30, new Random(1));
        world.paint(10, 2, 0, Material.SAND);
        step(world, 40);
        expect(world.materialAt(10, 29) == Material.SAND, "sand should settle on the floor");
    }

    private static void sandSinksThroughWater() {
        World world = new World(12, 16, new Random(2));
        for (int y = 8; y < 16; y++) {
            world.paint(6, y, 0, Material.WATER);
        }
        world.paint(6, 6, 0, Material.SAND);
        step(world, 30);
        expect(world.materialAt(6, 15) == Material.SAND, "sand should sink under water");
    }

    private static void waterSpreads() {
        World world = new World(25, 10, new Random(3));
        for (int x = 0; x < world.width; x++) {
            world.paint(x, 9, 0, Material.STONE);
        }
        for (int y = 4; y <= 8; y++) {
            world.paint(12, y, 0, Material.WATER);
        }
        step(world, 30);
        int spread = 0;
        for (int x = 0; x < world.width; x++) {
            if (world.materialAt(x, 8) == Material.WATER) {
                spread++;
            }
        }
        expect(spread > 1, "water should spread along the floor");
    }

    private static void lavaMeetsWater() {
        World world = new World(10, 10, new Random(4));
        world.paint(4, 5, 0, Material.LAVA);
        world.paint(5, 5, 0, Material.WATER);
        step(world, 8);
        boolean reacted = world.count(Material.STEAM) + world.count(Material.STONE) > 0;
        expect(reacted, "lava meeting water should make steam or stone");
    }

    private static void fireBurnsWood() {
        World world = new World(8, 8, new Random(5));
        world.paint(3, 4, 0, Material.WOOD);
        world.paint(4, 4, 0, Material.FIRE);
        step(world, 25);
        expect(world.materialAt(3, 4) != Material.WOOD, "fire should consume adjacent wood");
    }

    private static void gunpowderDetonates() {
        World world = new World(21, 12, new Random(6));
        for (int x = 4; x <= 16; x++) {
            world.paint(x, 6, 0, Material.GUNPOWDER);
        }
        world.paint(4, 5, 0, Material.FIRE);
        step(world, 12);
        expect(world.count(Material.FIRE) > 3, "gunpowder should burst into fire");
    }

    private static void acidEatsStone() {
        World world = new World(8, 8, new Random(7));
        world.paint(4, 5, 0, Material.STONE);
        world.paint(3, 4, 0, Material.STONE);
        world.paint(5, 4, 0, Material.STONE);
        world.paint(4, 4, 0, Material.ACID);
        step(world, 40);
        expect(world.count(Material.STONE) < 3, "acid should dissolve stone while pooled against it");
    }

    private static void step(World world, int times) {
        for (int i = 0; i < times; i++) {
            world.step();
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
