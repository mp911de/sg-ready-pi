package cat.joanpujol.smasolar.modbus;

public enum ModbusDataType {
  S16(0x8000, "Signed word (16 bits)", Short.class, 2),
  S32(0x80000000, "Signed double word (32 bits)", Integer.class, 4),
  STR32(0x0, "32 byte  data field in UTF8", String.class, 32),
  U16(0xFFFF, "Unsigned word (16 bits)", Integer.class, 2),
  U32(0xFFFFFFFF, "Unsigned double word (32 bits)", Long.class, 4),
  U32_STATUS(0xFFFFFD, "Double word where only lower 24 bits are used", Integer.class, 4),
  U64(Long.parseUnsignedLong("FFFFFFFFFFFFFFFF", 16), "Quadruple word (64 bits)", Long.class, 8);

  private long nullValue;
  private String description;
  private Class type;
  private int length;

  ModbusDataType(long nullValue, String description, Class type, int length) {
    this.nullValue = nullValue;
    this.description = description;
    this.type = type;
    this.length = length;
  }

  public long getNullValue() {
    return nullValue;
  }

  public String getDescription() {
    return description;
  }

  public int getLength() {
    return length;
  }

  public Class getType() {
    return type;
  }
}
