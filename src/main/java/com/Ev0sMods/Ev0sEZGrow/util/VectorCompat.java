package com.Ev0sMods.Ev0sEZGrow.util;

import com.hypixel.hytale.component.spatial.SpatialStructure;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compatibility shim for Vector3d-typed APIs that changed between Hytale versions:
 *   release     → com.hypixel.hytale.math.vector.Vector3d
 *   pre-release → org.joml.Vector3d
 *
 * Compiled against pre-release (org.joml path is a direct call).
 * On release the Hytale path is reached via cached reflection handles.
 */
public final class VectorCompat {

    private static final boolean USE_JOML;

    private static final Constructor<?> HYTALE_VEC3D_CTOR;
    private static final Method SPATIAL_COLLECT_CYLINDER;
    private static final Method BLOCK_TYPE_GET_BLOCK_CENTER;
    private static final Field  HYTALE_VEC3D_X;
    private static final Field  HYTALE_VEC3D_Y;
    private static final Field  HYTALE_VEC3D_Z;

    private static final Constructor<?> HYTALE_VEC3I_CTOR;
    private static final Method CHUNK_GET_BLOCK_TYPE;

    static {
        boolean joml = true;
        Constructor<?> vc   = null;
        Method cc   = null;
        Method gbc  = null;
        Field  fx   = null, fy = null, fz = null;
        Constructor<?> vic  = null;
        Method cgbt = null;

        try {
            for (Method m : SpatialStructure.class.getMethods()) {
                if (m.getName().equals("collectCylinder")) {
                    joml = m.getParameterTypes()[0].getName().startsWith("org.joml");
                    break;
                }
            }
            if (!joml) {
                Class<?> hv  = Class.forName("com.hypixel.hytale.math.vector.Vector3d");
                Class<?> hvi = Class.forName("com.hypixel.hytale.math.vector.Vector3i");
                vc   = hv.getConstructor(double.class, double.class, double.class);
                cc   = SpatialStructure.class.getMethod("collectCylinder", hv, double.class, double.class, List.class);
                gbc  = BlockType.class.getMethod("getBlockCenter", int.class, hv);
                fx   = hv.getField("x");
                fy   = hv.getField("y");
                fz   = hv.getField("z");
                vic  = hvi.getConstructor(int.class, int.class, int.class);
                cgbt = WorldChunk.class.getMethod("getBlockType", hvi);
            }
        } catch (Throwable t) {
            joml = true;
        }

        USE_JOML                    = joml;
        HYTALE_VEC3D_CTOR           = vc;
        SPATIAL_COLLECT_CYLINDER    = cc;
        BLOCK_TYPE_GET_BLOCK_CENTER = gbc;
        HYTALE_VEC3D_X              = fx;
        HYTALE_VEC3D_Y              = fy;
        HYTALE_VEC3D_Z              = fz;
        HYTALE_VEC3I_CTOR           = vic;
        CHUNK_GET_BLOCK_TYPE        = cgbt;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void collectCylinder(
            SpatialStructure<T> spatial,
            double x, double y, double z,
            double radius, double height,
            List<T> out) {
        try {
            if (USE_JOML) {
                spatial.collectCylinder(new org.joml.Vector3d(x, y, z), radius, height, out);
            } else {
                Object hv = HYTALE_VEC3D_CTOR.newInstance(x, y, z);
                SPATIAL_COLLECT_CYLINDER.invoke(spatial, hv, radius, height, out);
            }
        } catch (Throwable t) {
            throw new RuntimeException("VectorCompat.collectCylinder failed", t);
        }
    }

    public static double[] getBlockCenter(BlockType blockType, int rotationIndex) {
        try {
            if (USE_JOML) {
                org.joml.Vector3d v = new org.joml.Vector3d();
                blockType.getBlockCenter(rotationIndex, v);
                return new double[]{v.x, v.y, v.z};
            } else {
                Object hv = HYTALE_VEC3D_CTOR.newInstance(0.0, 0.0, 0.0);
                BLOCK_TYPE_GET_BLOCK_CENTER.invoke(blockType, rotationIndex, hv);
                return new double[]{
                    (double) HYTALE_VEC3D_X.get(hv),
                    (double) HYTALE_VEC3D_Y.get(hv),
                    (double) HYTALE_VEC3D_Z.get(hv)
                };
            }
        } catch (Throwable t) {
            throw new RuntimeException("VectorCompat.getBlockCenter failed", t);
        }
    }

    public static BlockType getBlockType(WorldChunk chunk, int x, int y, int z) {
        try {
            if (USE_JOML) {
                return chunk.getBlockType(new org.joml.Vector3i(x, y, z));
            } else {
                Object hvi = HYTALE_VEC3I_CTOR.newInstance(x, y, z);
                return (BlockType) CHUNK_GET_BLOCK_TYPE.invoke(chunk, hvi);
            }
        } catch (Throwable t) {
            throw new RuntimeException("VectorCompat.getBlockType failed", t);
        }
    }

    private VectorCompat() {}
}
