package cat.joanpujol.smasolar.modbus;

import cat.joanpujol.smasolar.error.UnnexpectedCodition;
import java.util.*;

public class SmaModbusRequest {
  public enum Type {
    READ,
    WRITE
  }

  static final int READ_MAX_REGISTERS_NUMBER = 125;
  static final int WRITE_MAX_REGISTERS_NUMBER = 123;

  private Type requestType;
  private Integer unitId;
  private List<ModbusRegister> registers = new ArrayList<>();

  private boolean atomicRequestsCalculated = false;
  private List<SmaModbusRequest> atomicRequests = Collections.emptyList();

  private SmaModbusRequest(Builder builder) {
    unitId = builder.unitId;
    registers = builder.registers;
    requestType = builder.requestType;
  }

  private SmaModbusRequest() {}

  public static Builder newBuilder(Type requestType) {
    return new Builder(requestType);
  }

  public List<ModbusRegister> getRegisters() {
    return Collections.unmodifiableList(registers);
  }

  public Integer getUnitId() {
    return unitId;
  }

  public Type getRequestType() {
    return requestType;
  }

  public int getNumberOfNeededRequests() {
    if (!atomicRequestsCalculated) calculateAtomicRequests();
    return atomicRequests.size();
  }

  public boolean isAtomic() {
    if (!atomicRequestsCalculated) calculateAtomicRequests();
    return atomicRequests.size() <= 1;
  }

  public List<SmaModbusRequest> subdivideInAtomicRequests() {
    if (!atomicRequestsCalculated) calculateAtomicRequests();
    return atomicRequests;
  }

  private void calculateAtomicRequests() {
    atomicRequests = new AtomicRequestsCalculator(this).calculate();
    atomicRequestsCalculated = true;
  }

  private SmaModbusRequest createCopyWithoutRegisters() {
    SmaModbusRequest copy = new SmaModbusRequest();
    copy.requestType = requestType;
    copy.unitId = unitId;
    return copy;
  }

  /**
   * Calculate number of registers that will be read to execute this request that must be an atomic
   * one
   *
   * @throws IllegalStateException if this request isn't atomic
   * @return number of registers that will be readed
   */
  public int calculateNumberOfRegistersToReadInAtomicRequest() {
    if (isAtomic()) {
      ModbusRegister last = registers.get(registers.size() - 1);
      ModbusRegister first = registers.get(0);
      return last.getRegisterNumber()
          - first.getRegisterNumber()
          + last.getDataType().getLength() / 2;
    } else {
      throw new IllegalStateException(
          "Can't calculate number of registers to read if request is not atomic");
    }
  }

  public int getFirstRegisterNumber() {
    if (!registers.isEmpty()) return registers.get(0).getRegisterNumber();
    else throw new IndexOutOfBoundsException("This request hasn't any registers");
  }

  public static final class Builder {
    private Integer unitId;
    private List<ModbusRegister> registers = new ArrayList<>();
    private Type requestType;

    public Builder(Type requestType) {
      this.requestType = requestType;
    }

    public Builder unitId(Integer unitId) {
      this.unitId = unitId;
      return this;
    }

    public Builder addRegister(ModbusRegister register) {
      registers.add(register);
      return this;
    }

    public SmaModbusRequest build() {
      return new SmaModbusRequest(this);
    }
  }

  private static class AtomicRequestsCalculator {
    private SmaModbusRequest originalRequest;

    public AtomicRequestsCalculator(SmaModbusRequest originalRequest) {
      this.originalRequest = originalRequest;
    }

    public List<SmaModbusRequest> calculate() {
      List<ModbusRegister> registers = originalRequest.registers;
      if (registers.size() <= 1) {
        return List.of(originalRequest);
      } else {
        Collections.sort(registers, Comparator.comparing(ModbusRegister::getRegisterNumber));
        ModbusRegister last = registers.get(registers.size() - 1);
        ModbusRegister first = registers.get(0);
        int numberOfRegistersToRead =
            last.getRegisterNumber()
                - first.getRegisterNumber()
                + last.getDataType().getLength() / 2;
        int maxNumberOfregisters;
        maxNumberOfregisters = getMaxNumberOfRegisters(originalRequest);
        if (numberOfRegistersToRead > maxNumberOfregisters) {
          return divideIntoAtomicRequests();
        } else {
          return List.of(originalRequest);
        }
      }
    }

    private static int getMaxNumberOfRegisters(SmaModbusRequest request) {
      int maxNumberOfregisters;
      switch (request.getRequestType()) {
        case READ:
          maxNumberOfregisters = SmaModbusRequest.READ_MAX_REGISTERS_NUMBER;
          break;
        case WRITE:
          maxNumberOfregisters = SmaModbusRequest.WRITE_MAX_REGISTERS_NUMBER;
          break;
        default:
          throw new UnnexpectedCodition("Request type " + request.getRequestType() + " unexpected");
      }
      return maxNumberOfregisters;
    }

    private List<SmaModbusRequest> divideIntoAtomicRequests() {
      List<SmaModbusRequest> requests = new ArrayList<>();
      SmaModbusRequest current = originalRequest.createCopyWithoutRegisters();
      requests.add(current);

      for (ModbusRegister register : originalRequest.getRegisters()) {
        if (!isAtomicIfIAdd(current, register)) {
          current = originalRequest.createCopyWithoutRegisters();
          requests.add(current);
        }
        current.registers.add(register);
      }

      requests.forEach(
          r -> {
            r.atomicRequestsCalculated = true;
            r.atomicRequests = List.of(r);
          });

      return requests;
    }

    private boolean isAtomicIfIAdd(SmaModbusRequest request, ModbusRegister registerToAdd) {
      if (request.getRegisters().isEmpty()) {
        return true;
      } else {
        ModbusRegister first = request.getRegisters().get(0);
        int registerCount =
            registerToAdd.getRegisterNumber()
                - first.getRegisterNumber()
                + registerToAdd.getDataType().getLength() / 2;
        return registerCount <= getMaxNumberOfRegisters(request);
      }
    }
  }
}
