package cat.joanpujol.smasolar.modbus;

import com.digitalpetri.modbus.master.ModbusTcpMaster;
import com.digitalpetri.modbus.master.ModbusTcpMasterConfig;
import com.digitalpetri.modbus.requests.ReadInputRegistersRequest;
import com.digitalpetri.modbus.responses.ReadInputRegistersResponse;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/** Client to read modbus registers on SMA devices */
public class SmaModbusClient implements ModbusRegisterReader {
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
  private String address;
  private int port;
  private SmaModbusDevice device;

  private ModbusTcpMaster modbusClient;

  public SmaModbusClient(String address, int port, SmaModbusDevice smaDevice) {
    this.address = address;
    this.port = port;
    this.device = smaDevice;
  }

  private ModbusTcpMaster ensureModbusClient() {
    if (modbusClient == null) modbusClient = createModbusClient();
    return modbusClient;
  }

  protected ModbusTcpMaster createModbusClient() {
    ModbusTcpMasterConfig.Builder configurationBuilder =
        new ModbusTcpMasterConfig.Builder(address).setPort(port).setTimeout(DEFAULT_TIMEOUT);
    customizeModbusConfiguration(configurationBuilder);
    ModbusTcpMasterConfig config = configurationBuilder.build();
    return new ModbusTcpMaster(config);
  }

  /**
   * Allows to customize modbus client configuration. This methods is called with builder previously
   * configurated with default values
   *
   * @param builder client configuration builder
   */
  protected void customizeModbusConfiguration(ModbusTcpMasterConfig.Builder builder) {}

  public Completable connect() {
    return Completable.fromFuture(ensureModbusClient().connect());
  }

  public Completable disconnect() {
    return Completable.fromFuture(ensureModbusClient().disconnect());
  }

  public <T> Single<T> readRegister(ModbusRegister<T> reg) {
    return readRegister(reg, device.calculateMainUnitId(null));
  }

  public <T> Single<T> readRegister(ModbusRegister<T> reg, int unitId) {
    var request =
        new ReadInputRegistersRequest(reg.getRegisterNumber(), reg.getDataType().getLength() / 2);
    var futureResult = ensureModbusClient().sendRequest(request, unitId);
    return Single.fromFuture(futureResult)
        .map(
            response -> {
              ByteBuf registers = ((ReadInputRegistersResponse) response).getRegisters();
              var value =
                  new ModbusValueReader().read(registers, reg.getDataType(), reg.getDataFormat());
              ReferenceCountUtil.release(response);
              return value;
            });
  }

  public Single<SmaModbusResponse> read(SmaModbusRequest request) {
    if (request.isAtomic()) {
      return readAtomicRequest(request);
    } else {
      List<SmaModbusRequest> atomicRequests = request.subdivideInAtomicRequests();
      var observables =
          atomicRequests.stream().map(ar -> readAtomicRequest(ar)).collect(Collectors.toList());
      return Single.zip(
          observables,
          (Object[] responses) -> {
            SmaModbusResponse joinedResponse = new SmaModbusResponse();
            for (Object requestResponse : responses) {
              SmaModbusResponse typedResponse = (SmaModbusResponse) requestResponse;
              typedResponse
                  .getAllRegisters()
                  .forEach((k, v) -> joinedResponse.setRegisterValue(k, v));
            }
            return joinedResponse;
          });
    }
  }

  private Single<SmaModbusResponse> readAtomicRequest(SmaModbusRequest request) {
    var modbusreq =
        new ReadInputRegistersRequest(
            request.getFirstRegisterNumber(),
            request.calculateNumberOfRegistersToReadInAtomicRequest());
    var futureResult =
        ensureModbusClient().sendRequest(modbusreq, getUnitIdToUseForRequest(request));
    return Single.fromFuture(futureResult)
        .map(
            modbusResponse -> {
              ByteBuf registers = ((ReadInputRegistersResponse) modbusResponse).getRegisters();
              var response = createRequestResponse(request, registers);
              ReferenceCountUtil.release(response);
              return response;
            });
  }

  private Integer getUnitIdToUseForRequest(SmaModbusRequest request) {
    Integer unitId = request.getUnitId();
    if (unitId != null) {
      return unitId;
    } else {
      return device.calculateMainUnitId(null);
    }
  }

  private SmaModbusResponse createRequestResponse(SmaModbusRequest request, ByteBuf registers) {
    int firstRegister = request.getFirstRegisterNumber();
    var response = new SmaModbusResponse();
    var modbusValueReader = new ModbusValueReader();
    int currentPos = 0;
    for (ModbusRegister register : request.getRegisters()) {
      int pos = register.getRegisterNumber() - firstRegister;
      if (pos != currentPos) registers.skipBytes((pos - currentPos) * 2);
      var value =
          modbusValueReader.read(registers, register.getDataType(), register.getDataFormat());
      response.setRegisterValue(register, value);
      currentPos =
          pos
              + register.getDataType().getLength()
                  / 2; // FIXME: When reading null terminated STR this possibly isn't correct
    }
    return response;
  }
}
