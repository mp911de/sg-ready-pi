package cat.joanpujol.smasolar.modbus;

import reactor.core.publisher.Mono;

/** Interface to read a single register. Used to do IoC on SmaModbusClient. */
public interface ModbusRegisterReader {
	<T> Mono<T> readRegister(ModbusRegister<T> register, int unitId);
}
