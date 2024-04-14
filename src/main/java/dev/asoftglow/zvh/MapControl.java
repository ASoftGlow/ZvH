package dev.asoftglow.zvh;

import org.bukkit.Bukkit;

public class MapControl {

    // stores the two corners of the game arena (x1,z1,x2,z2,y)
    final static int[] bounds = { -49, 16, 53, 68, 1 };

    // draws a circle, in a block game!
    public static void circle(int x, int y, int z, int radius, String material, boolean filled, int precision){

        double angle = 0;

        // Loop through points around the circle
        for (int i = 0; i < precision; i++){

            // update angle
            angle += 2 * Math.PI / precision;

            // get coords of nearest block
            int dX = x + (int) Math.round(radius * Math.cos(angle));
            int dZ = z + (int) Math.round(radius * Math.sin(angle));

            if (filled == true){

                // fill in between (dX,dZ) and (x,y)
                fill(x, y, z, dX, y, dZ, material);

            } else {

                // setBlock at (dX,dZ)
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                "setblock " + dX + " " + y + " " + dZ + " " + material);
            }
        }

    }

    // generate a zombie safe zone in the map
    public static void genZSafe(int x, int y, int z, int radius){
        circle(x, y, z, radius, "stone"/*"light_gray_concrete_powder"*/, true, radius * radius * 3);
    }

    // set the corners of the game arena
    public static void setBounds(int x1, int z1, int x2, int z2, int y) {
        bounds[0] = x1;
        bounds[1] = z1;
        bounds[2] = x2;
        bounds[3] = z2;
        bounds[4] = y;
    }

    //vanilla fill
    public static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String material){
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2
                        + " " + z2 + material);
    }

    // fill in columns to avoid the max fill size
    public static void infFill(int x1, int y1, int z1, int x2, int y2, int z2, String material){
        
        // Calculate best fill column size
        int maxProduct = Math.round(32768 / (y2 - y1));
        int maxX = (x2 - x1);
        int maxZ = (z2 - z1);
        int columnX = 1;
        int columnZ = 1;
        
        for (int i = 1; i <= maxX || i <= maxZ; i++) {
            
            // X gets highest priority
            if (maxX % i == 0 && (i * columnZ) <= maxProduct) {
                columnX = i;
            }

            // Z gets lower priority
            if (maxZ % i == 0 && (i * columnX) <= maxProduct) {
                columnZ = i;
            }
        }

        // Iterate through all layers
        for (int z = z1; z < z2; z += columnZ){
                
            // Iterate through all columns
            for (int x = x1; x < x2; x += columnX){
                
                fill(x, y1, z, (x + columnX), y2, (z + columnZ), material);
            }
        }
    }

    // clone in columns to avoid the max clone size
    public static void infClone(int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3){
        
        // Calculate best clone column size
        int maxProduct = Math.round(32768 / (y2 - y1));
        int maxX = (x2 - x1);
        int maxZ = (z2 - z1);
        int columnX = 1;
        int columnZ = 1;
        
        for (int i = 1; i <= maxX || i <= maxZ; i++) {
            
            // X gets highest priority
            if (maxX % i == 0 && (i * columnZ) <= maxProduct) {
                columnX = i;
            }

            // Z gets lower priority
            if (maxZ % i == 0 && (i * columnX) <= maxProduct) {
                columnZ = i;
            }
        }

        // vars
        int zC;
        int xC;

        // Iterate through all layers
        for (int z = z1; z < z2; z += columnZ){
            zC = z3 + z - z1;

            // Iterate through all columns
            for (int x = x1; x < x2; x += columnX){
                xC = x3 + x - x1;

                // Send clone command
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                    "clone %d %d %d %d %d %d %d %d %d replace".formatted(
                    x, y1, z,
                    (x + columnX), y2, (z + columnZ),
                    xC, y3, zC));
            }
        }
    }

    // reset the game map based on the bounds defined by setBounds
    public static void resetMap() {
        
        // Clear building space
        infFill(bounds[0], bounds[4] + 1, bounds[1], bounds[2], bounds[4] + 30, bounds[3], "air");
        
        // Floor
        fill(bounds[0], bounds[4] - 1, bounds[1], bounds[2], bounds[4] - 1, bounds[3], "stone");
        fill(bounds[0], bounds[4], bounds[1], bounds[2], bounds[4], bounds[3], "gray_concrete_powder");

        // Border
        fill(bounds[0], bounds[4], bounds[1], bounds[0], bounds[4], bounds[3], "smooth_quartz");
        fill(bounds[0], bounds[4], bounds[3], bounds[2], bounds[4], bounds[3], "smooth_quartz");
        fill(bounds[2], bounds[4], bounds[3], bounds[2], bounds[4], bounds[1], "smooth_quartz");
        fill(bounds[2], bounds[4], bounds[1], bounds[0], bounds[4], bounds[1], "smooth_quartz");
        
        // Barriers
        fill(bounds[0], bounds[4] + 1, bounds[1], bounds[0], bounds[4] + 31, bounds[3], "barrier");
        fill(bounds[0], bounds[4] + 1, bounds[3], bounds[2], bounds[4] + 31, bounds[3], "barrier");
        fill(bounds[2], bounds[4] + 1, bounds[3], bounds[2], bounds[4] + 31, bounds[1], "barrier");
        fill(bounds[2], bounds[4] + 1, bounds[1], bounds[0], bounds[4] + 31, bounds[1], "barrier");
        fill(bounds[0], bounds[4] + 31, bounds[1], bounds[2], bounds[4] + 31, bounds[3], "barrier");

        // Generate zombie safe zone
        genZSafe(37, 1, 42, 6);
    }
}