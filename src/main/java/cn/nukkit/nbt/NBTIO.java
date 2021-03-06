package cn.nukkit.nbt;

import cn.nukkit.item.Item;
import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;

import java.io.*;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NBTIO {
    /**
     * A Named Binary Tag library for Nukkit Project
     */

    public static CompoundTag putItemHelper(Item item) {
        return putItemHelper(item, null);
    }

    public static CompoundTag putItemHelper(Item item, Integer slot) {
        CompoundTag tag = new CompoundTag(null)
                .putShort("id", item.getId())
                .putByte("Count", (byte) item.getCount())
                .putShort("Damage", item.getDamage());
        if (slot != null) {
            tag.putByte("Slot", (byte) (int) slot);
        }

        if (item.hasCompoundTag()) {
            tag.putCompound("tag", item.getNamedTag());
        }

        return tag;
    }

    public static Item getItemHelper(CompoundTag tag) {
        if (!tag.contains("id") || !tag.contains("Count")) {
            return Item.get(0);
        }

        Item item = Item.get(tag.getShort("id"), !tag.contains("Damage") ? 0 : tag.getShort("Damage"), tag.getByte("Count"));

        if (tag.contains("tag") && tag.get("tag") instanceof CompoundTag) {
            item.setNamedTag(tag.getCompound("tag"));
        }

        return item;
    }

    public static CompoundTag read(File file) throws IOException {
        return read(file, ByteOrder.BIG_ENDIAN);
    }

    public static CompoundTag read(File file, ByteOrder endianness) throws IOException {
        if (!file.exists()) return null;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return read(fileInputStream, endianness);
        }
    }

    public static CompoundTag read(InputStream inputStream) throws IOException {
        return read(inputStream, ByteOrder.BIG_ENDIAN);
    }

    public static CompoundTag read(InputStream inputStream, ByteOrder endianness) throws IOException {
        try (NBTInputStream nbtInputStream = new NBTInputStream(inputStream, endianness)) {
            Tag tag = Tag.readNamedTag(nbtInputStream);
            if (tag instanceof CompoundTag) {
                return (CompoundTag) tag;
            }
            throw new IOException("Root tag must be a named compound tag");
        }
    }

    public static CompoundTag read(byte[] data) throws IOException {
        return read(data, ByteOrder.BIG_ENDIAN);
    }

    public static CompoundTag read(byte[] data, ByteOrder endianness) throws IOException {
        return read(new ByteArrayInputStream(data), endianness);
    }

    public static CompoundTag readCompressed(InputStream in) throws IOException {
        return readCompressed(in, ByteOrder.BIG_ENDIAN);
    }

    public static CompoundTag readCompressed(InputStream in, ByteOrder endianness) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(in)) {
            return read(gzipInputStream, endianness);
        }
    }

    public static CompoundTag readCompressed(byte[] data) throws IOException {
        return readCompressed(data, ByteOrder.BIG_ENDIAN);
    }

    public static CompoundTag readCompressed(byte[] data, ByteOrder endianness) throws IOException {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return read(gzipInputStream, endianness);
        }
    }

    public static byte[] write(CompoundTag tag) throws IOException {
        return write(tag, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] write(CompoundTag tag, ByteOrder endianness) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (NBTOutputStream stream = new NBTOutputStream(baos, endianness)) {
            Tag.writeNamedTag(tag, stream);
            return baos.toByteArray();
        }
    }

    public static byte[] write(Collection<CompoundTag> tags) throws IOException {
        return write(tags, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] write(Collection<CompoundTag> tags, ByteOrder endianness) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (NBTOutputStream stream = new NBTOutputStream(baos, endianness)) {
            for (CompoundTag tag : tags) {
                Tag.writeNamedTag(tag, stream);
            }
            return baos.toByteArray();
        }
    }

    public static void write(CompoundTag tag, File file) throws IOException {
        write(tag, file, ByteOrder.BIG_ENDIAN);
    }

    public static void write(CompoundTag tag, File file, ByteOrder endianness) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            write(tag, fileOutputStream, endianness);
        }
    }

    public static void write(CompoundTag tag, OutputStream outputStream) throws IOException {
        write(tag, outputStream, ByteOrder.BIG_ENDIAN);
    }

    public static void write(CompoundTag tag, OutputStream outputStream, ByteOrder endianness) throws IOException {
        try (NBTOutputStream nbtOutputStream = new NBTOutputStream(outputStream, endianness)) {
            Tag.writeNamedTag(tag, nbtOutputStream);
        }
    }

    public static byte[] writeGZIPCompressed(CompoundTag tag) throws IOException {
        return writeGZIPCompressed(tag, ByteOrder.BIG_ENDIAN);
    }

    public static byte[] writeGZIPCompressed(CompoundTag tag, ByteOrder endianness) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeGZIPCompressed(tag, baos, endianness);
        return baos.toByteArray();
    }

    public static void writeGZIPCompressed(CompoundTag tag, OutputStream out) throws IOException {
        writeGZIPCompressed(tag, out, ByteOrder.BIG_ENDIAN);
    }

    public static void writeGZIPCompressed(CompoundTag tag, OutputStream out, ByteOrder endianness) throws IOException {
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            write(tag, gzip, endianness);
        }
    }

    public static void writeZLIBCompressed(CompoundTag tag, OutputStream out) throws IOException {
        writeZLIBCompressed(tag, out, ByteOrder.BIG_ENDIAN);
    }

    public static void writeZLIBCompressed(CompoundTag tag, OutputStream out, ByteOrder endianness) throws IOException {
        writeZLIBCompressed(tag, out, Deflater.DEFAULT_COMPRESSION, endianness);
    }

    public static void writeZLIBCompressed(CompoundTag tag, OutputStream out, int level) throws IOException {
        writeZLIBCompressed(tag, out, level, ByteOrder.BIG_ENDIAN);
    }

    public static void writeZLIBCompressed(CompoundTag tag, OutputStream out, int level, ByteOrder endianness) throws IOException {
        try (DeflaterOutputStream outputStream = new DeflaterOutputStream(out, new Deflater(level))) {
            write(tag, outputStream, endianness);
        }
    }

    public static void safeWrite(CompoundTag tag, File file) throws IOException {
        File tmpFile = new File(file.getAbsolutePath() + "_tmp");
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        write(tag, tmpFile);
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Failed to delete " + file);
            }
        }
        tmpFile.renameTo(file);
    }
}
