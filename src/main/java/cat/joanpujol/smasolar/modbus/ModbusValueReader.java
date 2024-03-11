package cat.joanpujol.smasolar.modbus;

import static cat.joanpujol.smasolar.modbus.ModbusDataType.U64;

import io.netty.buffer.ByteBuf;
import io.netty.util.ByteProcessor;
import io.netty.util.CharsetUtil;
import java.math.BigInteger;

public class ModbusValueReader {

  public <T> T read(ByteBuf from, ModbusDataType dataType, ModbusDataFormat<T> format) {
    Object value = readValue(from, dataType);
    return formatValue(value, format);
  }

  private Object readValue(ByteBuf from, ModbusDataType dataType) {
    switch (dataType) {
      case S16:
        short s16value = from.readShort();
        return s16value != Short.MIN_VALUE ? s16value : null;
      case U16:
        int u16value = from.readUnsignedShort();
        return u16value != 0xFFFF ? u16value : null;
      case S32:
        int s32value = from.readInt();
        return s32value != Integer.MIN_VALUE ? s32value : null;
      case U32:
        long u32value = from.readUnsignedInt();
        return u32value != 0xFFFFFFFFL ? u32value : null;
      case U32_STATUS:
        long u32svalue = from.readUnsignedInt();
        return u32svalue != 0xFFFFFD ? u32svalue : null;
      case U64:
        long u64value = from.readLong();
        if (u64value != U64.getNullValue()) {
          String slong = Long.toUnsignedString(u64value);
          return new BigInteger(slong);
        } else {
          return null;
        }
      case STR32:
        var strvalue = from.readBytes(32);
        int firstNull = strvalue.forEachByte(ByteProcessor.FIND_NUL);
        if (firstNull == 0) return null;
        else if (firstNull != -1) return strvalue.slice(0, firstNull).toString(CharsetUtil.UTF_8);
        else return strvalue.toString(CharsetUtil.UTF_8);
      default:
        throw new IllegalArgumentException("Unrecognized " + dataType);
    }
  }

  private <T> T formatValue(Object value, ModbusDataFormat<T> format) {
    return format.getFormatDecoder().format(value);
  }
}
