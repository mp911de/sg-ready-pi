package cat.joanpujol.smasolar.modbus;

/** This interface represents a SMA device */
public interface SmaModbusDevice {
  /**
   * Returns main unitId of device
   *
   * @param registerReader A register reader that can be used to calculate main unitId
   * @return main unitId
   */
  int calculateMainUnitId(ModbusRegisterReader registerReader);
}
