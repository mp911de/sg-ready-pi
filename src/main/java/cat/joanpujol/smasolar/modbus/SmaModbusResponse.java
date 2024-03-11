package cat.joanpujol.smasolar.modbus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SmaModbusResponse {
  private Map<ModbusRegister, Object> results = new HashMap<>();

  public Map<ModbusRegister, Object> getAllRegisters() {
    return Collections.unmodifiableMap(results);
  }

  public <T> void setRegisterValue(ModbusRegister<T> register, T value) {
    results.put(register, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T getRegisterValue(ModbusRegister<T> register) {
    return (T) results.get(register);
  }
}
