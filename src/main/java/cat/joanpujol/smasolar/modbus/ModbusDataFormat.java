package cat.joanpujol.smasolar.modbus;

import cat.joanpujol.smasolar.error.InvalidDataException;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

public class ModbusDataFormat<T> {

  private static final FormatDecoder<String> STRING_DECODER =
      new FormatDecoder<>() {
        @Override
        public String format(Object value) {
          if (value instanceof String) return (String) value;
          else if (value != null) return value.toString();
          else return null;
        }
      };

  private static final FormatDecoder<FirmwareVersion> FIRMWARE_DECODER =
      new FormatDecoder<>() {
        @Override
        public FirmwareVersion format(Object value) {
          if (value instanceof Number) {
            var versionAsU32 = Unpooled.copyInt((int) ((Number) value).longValue());
            var versionHexDump = ByteBufUtil.hexDump(versionAsU32);
            var major = Integer.parseInt(versionHexDump.substring(0, 2));
            var minor = Integer.parseInt(versionHexDump.substring(2, 4));
            var build = versionAsU32.skipBytes(2).readUnsignedByte();
            var release = versionAsU32.readUnsignedByte();
            return new FirmwareVersion(
                major, minor, build, FirmwareVersion.ReleaseType.fromNumberCode(release));
          } else
            throw new InvalidDataException(
                "Unnexpected type. Got " + value.getClass().getName() + " when expected a Number");
        }
      };

  private static final FormatDecoder<Number> FIX0_DECODER =
      new FormatDecoder<>() {
        @Override
        public Number format(Object value) {
          if (value instanceof Number) return (Number) value;
          else if (value == null) return null;
          else
            throw new InvalidDataException(
                "Unnexpected type. Got " + value.getClass().getName() + " when expected a Number");
        }
      };

  private static class DecimalFormatDecoder implements FormatDecoder<BigDecimal> {
    private final int numDecimals;

    public DecimalFormatDecoder(int numDecimals) {
      this.numDecimals = numDecimals;
    }

    @Override
    public Number format(Object value) {
      if (value instanceof Number) {
        Number num = (Number) value;
        return new BigDecimal(num.longValue()).divide(new BigDecimal(10).pow(numDecimals));
      } else if (value == null) {
        return null;
      } else
        throw new InvalidDataException(
            "Unnexpected type. Got " + value.getClass().getName() + " when expected a Number");
    }
  };

  private static final FormatDecoder<InetAddress> INET_ADDRESS_FORMAT_DECODER =
      new FormatDecoder<>() {
        @Override
        public InetAddress format(Object value) {
          if (value instanceof String) {
            try {
              return InetAddress.getByName((String) value);
            } catch (UnknownHostException e) {
              throw new InvalidDataException("Can't create Inet4Address from " + value, e);
            }
          } else if (value == null) return null;
          else
            throw new InvalidDataException(
                "Unnexpected type. Got " + value.getClass().getName() + " when expected a String");
        }
      };

  private static final FormatDecoder<Instant> INSTANT_FORMAT_DECODER =
      new FormatDecoder<>() {
        @Override
        public Instant format(Object value) {
          if (value instanceof Number) {
            return Instant.ofEpochSecond(((Number) value).longValue());
          } else if (value == null) return null;
          else
            throw new InvalidDataException(
                "Unnexpected type. Got " + value.getClass().getName() + " when expected a number");
        }
      };

  public static final ModbusDataFormat<String> ENUM =
      new ModbusDataFormat<>(
          "Coded numerical value (posible values indicated by each field",
          String.class,
          STRING_DECODER);
  public static final ModbusDataFormat<String> TAGLIST =
      new ModbusDataFormat<>(
          "Coded numerical value (posible values indicated by each field",
          String.class,
          STRING_DECODER);
  public static final ModbusDataFormat<Number> FIX0 =
      new ModbusDataFormat<>(
          "Decimal number, commercially rounded, without decimal place",
          Number.class,
          FIX0_DECODER);
  public static final ModbusDataFormat<BigDecimal> FIX1 =
      new ModbusDataFormat<>(
          "Decimal number, commercially rounded, one decimal place",
          BigDecimal.class,
          new DecimalFormatDecoder(1));
  public static final ModbusDataFormat<BigDecimal> FIX2 =
      new ModbusDataFormat<>(
          "Decimal number, commercially rounded, two decimal places",
          BigDecimal.class,
          new DecimalFormatDecoder(2));
  public static final ModbusDataFormat<BigDecimal> FIX3 =
      new ModbusDataFormat<>(
          "Decimal number, commercially rounded, three decimal places",
          BigDecimal.class,
          new DecimalFormatDecoder(3));
  public static final ModbusDataFormat<BigDecimal> FIX4 =
      new ModbusDataFormat<>(
          "Decimal number, commercially rounded, four decimal places",
          BigDecimal.class,
          new DecimalFormatDecoder(4));
  public static final ModbusDataFormat<String> FUNCTION_SEC =
      new ModbusDataFormat<>("Unused", String.class, STRING_DECODER);
  public static final ModbusDataFormat<FirmwareVersion> FW =
      new ModbusDataFormat<>("Firmware version", FirmwareVersion.class, FIRMWARE_DECODER);
  public static final ModbusDataFormat<Number> HW =
      new ModbusDataFormat<>("Hardware version", Number.class, FIX0_DECODER);
  public static final ModbusDataFormat<InetAddress> IP4 =
      new ModbusDataFormat<>(
          "4-byte IP address (IPv4) of the form XXX.XXX.XXX.XXX",
          InetAddress.class,
          INET_ADDRESS_FORMAT_DECODER);
  public static final ModbusDataFormat<String> RAW =
      new ModbusDataFormat<>(
          "Text or number. A RAW number has no decimal places and no thousand or other separation indicators",
          String.class,
          STRING_DECODER);
  public static final ModbusDataFormat<String> OUTLINE_PURCHASE_AGREEMENT =
      new ModbusDataFormat<>("Revision number of the form 2.3.4.5.", String.class, STRING_DECODER);
  public static final ModbusDataFormat<BigDecimal> TEMP =
      new ModbusDataFormat<>(
          "Temperature values are stored in special Modbus registers in degrees Celsius (°C), in degrees Fahrenheit (°F), or in Kelvin K. The values are commercially rounded, with one decimal place",
          BigDecimal.class,
          new DecimalFormatDecoder(1));
  public static final ModbusDataFormat<Instant> TM =
      new ModbusDataFormat<>("UTC time, in seconds", Instant.class, INSTANT_FORMAT_DECODER);
  public static final ModbusDataFormat<String> UTF8 =
      new ModbusDataFormat<>("Data in UTF8 format", String.class, STRING_DECODER);
  public static final ModbusDataFormat<Instant> DT =
      new ModbusDataFormat<>(
          "Date/time (Transmission in seconds since 1970-01-01)",
          Instant.class,
          INSTANT_FORMAT_DECODER);

  private String description;
  private Class<T> type;
  private FormatDecoder<T> formatDecoder;

  private ModbusDataFormat(String description, Class<T> type, FormatDecoder<T> formatDecoder) {
    this.description = description;
    this.type = type;
    this.formatDecoder = formatDecoder;
  }

  public String getDescription() {
    return description;
  }

  public Class<T> getType() {
    return type;
  }

  public FormatDecoder<T> getFormatDecoder() {
    return formatDecoder;
  }

  interface FormatDecoder<TD> {
    <TD> TD format(Object value);
  }
}
