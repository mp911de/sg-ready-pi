package cat.joanpujol.smasolar.modbus;

import io.reactivex.Single;

/** Interface to read a single register. Used to do IoC on SmaModbusClient. */
public interface ModbusRegisterReader {
  <T> Single<T> readRegister(ModbusRegister<T> register, int unitId);
}
