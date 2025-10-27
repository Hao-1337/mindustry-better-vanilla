package hao1337.addons;

import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.struct.ObjectIntMap;
import arc.struct.Queue;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Build;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.production.Drill;

import static mindustry.Vars.world;

public class Utils {

    /**
     * Gets the nearby tiles around the specified coordinates for a given block.
     *
     * @param x     The x-coordinate of the center tile.
     * @param y     The y-coordinate of the center tile.
     * @param block The block to calculate nearby tiles for.
     * @return A sequence of tiles surrounding the specified coordinates.
     */
    protected static Seq<Tile> getNearbyTiles(int x, int y, Block block) {
        return getNearbyTiles(x, y, block.size);
    }

    /**
     * Gets the nearby tiles around the specified coordinates for a given size.
     *
     * @param x    The x-coordinate of the center tile.
     * @param y    The y-coordinate of the center tile.
     * @param size The size of the area to check for nearby tiles.
     * @return A sequence of tiles surrounding the specified coordinates.
     */
    protected static Seq<Tile> getNearbyTiles(int x, int y, int size) {
        Seq<Tile> nearbyTiles = new Seq<>();

        Point2[] nearby = Edges.getEdges(size);
        for (Point2 point2 : nearby) {
            Tile t = world.tile(x + point2.x, y + point2.y);
            if (t != null) nearbyTiles.add(t);
        }

        return nearbyTiles;
    }

    /**
     * Gets the nearby tiles around the specified coordinates for two different sizes.
     * This method adjusts the offsets to ensure proper alignment.
     *
     * @param x     The x-coordinate of the center tile.
     * @param y     The y-coordinate of the center tile.
     * @param size1 The first size to calculate the offset.
     * @param size2 The second size to calculate the offset.
     * @return A sequence of tiles surrounding the specified coordinates.
     */
    protected static Seq<Tile> getNearbyTiles(int x, int y, int size1, int size2) {
        int offset1 = (size1 % 2 == 1 && size2 % 2 == 0) ? 1 : 0;
        int offset2 = ((size2 * 2 - 1) / 2);

        return getNearbyTiles(x - offset1, y - offset1, size1 + offset2);
    }

    /**
     * Counts the ores that can be mined by the specified drill on the given tile.
     *
     * @param tile  The tile to check for ores.
     * @param drill The drill used for mining.
     * @return An entry containing the item and its count, or null if no ores are found.
     */
    protected static ObjectIntMap.Entry<Item> countOre(Tile tile, Drill drill) {
        Item item;
        int count;

        ObjectIntMap<Item> oreCount = new ObjectIntMap<>();
        Seq<Item> itemArray = new Seq<>();

        for (Tile other : tile.getLinkedTilesAs(drill, new Seq<>())) {
            if (drill.canMine(other)) {
                oreCount.increment(drill.getDrop(other), 0, 1);
            }
        }

        for (Item i : oreCount.keys()) {
            itemArray.add(i);
        }

        itemArray.sort((item1, item2) -> {
            int type = Boolean.compare(!item1.lowPriority, !item2.lowPriority);
            if (type != 0) return type;
            int amounts = Integer.compare(oreCount.get(item1, 0), oreCount.get(item2, 0));
            if (amounts != 0) return amounts;
            return Integer.compare(item1.id, item2.id);
        });

        if (itemArray.size == 0) {
            return null;
        }

        item = itemArray.peek();
        count = oreCount.get(itemArray.peek(), 0);

        ObjectIntMap.Entry<Item> itemAndCount = new ObjectIntMap.Entry<>();
        itemAndCount.key = item;
        itemAndCount.value = count;

        return itemAndCount;
    }

    /**
     * Expands the area by the specified radius around the given sequence of tiles.
     *
     * @param tiles  The sequence of tiles to expand the area around.
     * @param radius The radius to expand the area by.
     */
    protected static void expandArea(Seq<Tile> tiles, int radius) {
        Seq<Tile> expandedTiles = new Seq<>();

        for (Tile tile : tiles) {
            for (int dx = -radius; dx < radius; dx++) {
                for (int dy = -radius; dy < radius; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    Tile nearby = tile.nearby(dx, dy);
                    if (nearby == null) continue;

                    if (!tiles.contains(nearby) && !expandedTiles.contains(nearby)) {
                        expandedTiles.add(nearby);
                    }
                }
            }
        }

        tiles.add(expandedTiles);
    }

    /**
     * Gets all connected tiles to the specified starting tile within a maximum limit.
     *
     * @param tile     The starting tile.
     * @param maxTiles The maximum number of connected tiles to retrieve.
     * @return A sequence of connected tiles.
     */
    protected static Seq<Tile> getConnectedTiles(Tile tile, int maxTiles) {
        Queue<Tile> queue = new Queue<>();
        Seq<Tile> tiles = new Seq<>();
        Seq<Tile> visited = new Seq<>();

        queue.addLast(tile);

        Item sourceItem = tile.drop();

        while (!queue.isEmpty() && tiles.size < maxTiles) {
            Tile currentTile = queue.removeFirst();

            if (!Build.validPlace(Blocks.copperWall.environmentBuildable() ? Blocks.copperWall : Blocks.berylliumWall, Vars.player.team(), currentTile.x, currentTile.y, 0) || visited.contains(currentTile))
                continue;

            if (currentTile.drop() == sourceItem) {
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        if (!(x == 0 && y == 0)) {
                            Tile neighbor = currentTile.nearby(x, y);
                            if (neighbor == null) continue;

                            if (!visited.contains(neighbor)) {
                                queue.addLast(neighbor);
                            }
                        }
                    }
                }

                tiles.add(currentTile);
            }

            visited.add(currentTile);
        }

        tiles.sort(Tile::pos);

        return tiles;
    }

    /**
     * Gets the rectangular area occupied by a block on the given tile.
     *
     * @param tile  The tile on which the block is placed.
     * @param block The block to get the rectangle for.
     * @return A rectangle representing the block's area.
     */
    protected static Rect getBlockRect(Tile tile, Block block) {
        int offset = (block.size - 1) / 2;
        return new Rect(tile.x - offset, tile.y - offset, block.size, block.size);
    }

    /**
     * Converts a tile's coordinates to a Point2 object.
     *
     * @param tile The tile to convert.
     * @return A Point2 object representing the tile's coordinates.
     */
    protected static Point2 tileToPoint2(Tile tile) {
        return new Point2(tile.x, tile.y);
    }
}