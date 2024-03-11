package cat.joanpujol.smasolar.modbus.devices;

import cat.joanpujol.smasolar.modbus.ModbusRegisterReader;
import cat.joanpujol.smasolar.modbus.SmaModbusDevice;

public class DefaultSmaModbusDevice implements SmaModbusDevice {

  @Override
  public int calculateMainUnitId(ModbusRegisterReader registerReader) {
    return 3; // By now use default value, dynamic query of the UnitId is not implemented
  }
}
