package com.wai.vaultapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;

/**
 * Encoder: provides reverse-based encryption.
 *
 * encryptReverse(src, dst) writes dst as the reverse of src (byte-wise).
 * This is reversible by running encryptReverse(dst, recoveredDst).
 */
public class Encoder {

    /**
     * Reverse-encrypt src into dst.
     * Uses RandomAccessFile to avoid loading whole file into memory.
     *
     * @param src source file (must exist)
     * @param dst destination file (will be created)
     * @return true on success
     */
    public static boolean encryptReverse(File src, File dst) {
        if (src == null || dst == null || !src.exists()) return false;
        final int CHUNK = 8192;
        RandomAccessFile raf = null;
        FileOutputStream fos = null;
        try {
            raf = new RandomAccessFile(src, "r");
            fos = new FileOutputStream(dst);

            long fileLen = raf.length();
            long remaining = fileLen;
            byte[] buffer = new byte[CHUNK];

            while (remaining > 0) {
                int toRead = (int)Math.min(CHUNK, remaining);
                long pos = remaining - toRead;
                raf.seek(pos);
                int read = raf.read(buffer, 0, toRead);
                if (read <= 0) break;

                // reverse the buffer content
                reverseInPlace(buffer, read);

                // write reversed chunk
                fos.write(buffer, 0, read);

                remaining -= read;
            }
            fos.getFD().sync();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (dst.exists()) dst.delete();
            return false;
        } finally {
            try {
                if (raf != null) raf.close();
            } catch (IOException ignored) {}
            try {
                if (fos != null) fos.close();
            } catch (IOException ignored) {}
        }
    }

    private static void reverseInPlace(byte[] b, int len) {
        int i = 0, j = len - 1;
        while (i < j) {
            byte t = b[i];
            b[i] = b[j];
            b[j] = t;
            i++; j--;
        }
    }
}
