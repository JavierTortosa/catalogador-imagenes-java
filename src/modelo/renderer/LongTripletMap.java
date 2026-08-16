package modelo.renderer;

/**
 * Mapa cerrado de tres longs a int con direccionamiento abierto y sondeo lineal.
 * Evita crear objetos de envoltura por clave (usado en la soldadura de vértices
 * y en la decimación por rejilla). Load factor máximo del 50 % para mantener el
 * sondeo en tiempo casi constante.
 */
final class LongTripletMap {

    private long[] kx;
    private long[] ky;
    private long[] kz;
    private int[] values;
    private byte[] state;
    private int size;
    private int mask;

    LongTripletMap(int expected) {
        int cap = 1;
        int need = Math.max(16, expected * 2);
        while (cap < need) cap <<= 1;
        allocate(cap);
    }

    private void allocate(int cap) {
        kx = new long[cap];
        ky = new long[cap];
        kz = new long[cap];
        values = new int[cap];
        state = new byte[cap];
        mask = cap - 1;
    }

    int size() {
        return size;
    }

    int get(long x, long y, long z) {
        int idx = hash(x, y, z);
        while (state[idx] == 1) {
            if (kx[idx] == x && ky[idx] == y && kz[idx] == z) {
                return values[idx];
            }
            idx = (idx + 1) & mask;
        }
        return -1;
    }

    void put(long x, long y, long z, int value) {
        if (size * 2 >= state.length) {
            grow();
        }
        int idx = hash(x, y, z);
        while (state[idx] == 1) {
            idx = (idx + 1) & mask;
        }
        kx[idx] = x;
        ky[idx] = y;
        kz[idx] = z;
        values[idx] = value;
        state[idx] = 1;
        size++;
    }

    private void grow() {
        long[] okx = kx, oky = ky, okz = kz;
        int[] oval = values;
        byte[] ost = state;
        int oldSize = size;
        allocate(state.length * 2);
        for (int i = 0; i < ost.length; i++) {
            if (ost[i] == 1) {
                int idx = hash(okx[i], oky[i], okz[i]);
                while (state[idx] == 1) {
                    idx = (idx + 1) & mask;
                }
                kx[idx] = okx[i];
                ky[idx] = oky[i];
                kz[idx] = okz[i];
                values[idx] = oval[i];
                state[idx] = 1;
            }
        }
        size = oldSize;
    }

    private int hash(long x, long y, long z) {
        // Mezcla multiplicativa con constantes primas impar y finalizador tipo
        // splitmix: dispersa incluso claves pequeñas (como los índices de celda
        // de la decimación, 0..~200) por toda la tabla. Con el hash anterior
        // (31*(31*x+y)+z) esas claves caían en el rango bajo del mask y el sondeo
        // lineal degeneraba a casi O(n²).
        long h = 0x9E3779B97F4A7C15L ^ x;
        h *= 0xC2B2AE3D27D4EB4FL;
        h ^= y;
        h *= 0x165667B19E3779F9L;
        h ^= z;
        h *= 0x85EBCA77C2B2AE63L;
        h ^= h >>> 32;
        return (int) h & mask;
    }

} // --- Fin de la clase LongTripletMap ---