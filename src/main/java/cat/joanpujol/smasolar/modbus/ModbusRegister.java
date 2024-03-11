package cat.joanpujol.smasolar.modbus;

import static cat.joanpujol.smasolar.modbus.ModbusAccesType.READ_ONLY;
import static cat.joanpujol.smasolar.modbus.ModbusDataFormat.*;
import static cat.joanpujol.smasolar.modbus.ModbusDataType.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * SMA modbus register
 *
 * @param <T>
 */
public class ModbusRegister<T> {

  public static final ModbusRegister<String> PROFILE_VERSION_NUMBER =
      new ModbusRegister<>(
          30001,
          "PROFILE_VERSION_NUMBER",
          "Version number of the SMA profile",
          U32,
          RAW,
          READ_ONLY);
  public static final ModbusRegister<String> SUSyID =
      new ModbusRegister<>(30003, "SUSyID", "Device ID", U32, RAW, READ_ONLY);
  public static final ModbusRegister<String> SERIAL_NUMBER =
      new ModbusRegister<>(30005, "SERIAL_NUMBER", "Serial number", U32, RAW, READ_ONLY);
  public static final ModbusRegister<String> MODBUS_DATA_CHANGE_COUNTER =
      new ModbusRegister<>(
          30007,
          "MODBUS_DATA_CHANGE_COUNTER",
          "Modbus data change: counter value will increase if data in the Profile has changed (overflow)",
          U32,
          RAW,
          READ_ONLY);
  public static final ModbusRegister<String> DEVICE_CLASS =
      new ModbusRegister<>(30051, "DEVICE_CLASS", "Device class", U32, ENUM, READ_ONLY);
  public static final ModbusRegister<Instant> UTC_SYSTEM_TIME_COMUNICATION_PRODUCT =
      new ModbusRegister<>(
          30193, "UTC_SYSTEM_TIME_COMUNICATION_PRODUCT", "UTC system time (s)", U32, DT, READ_ONLY);
  public static final ModbusRegister<String> DEVICE_STATUS =
      new ModbusRegister<>(30201, "DEVICE_STATUS", "Device status", U32, ENUM, READ_ONLY);
  public static final ModbusRegister<Number> MAXIMUM_POSSIBLE_PERMANENT_ACTIVE_POWER_LIMITATION =
      new ModbusRegister<>(
          30231,
          "MAXIMUM_POSSIBLE_PERMANENT_ACTIVE_POWER_LIMITATION",
          "Device status",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> PERMANENT_ACTIVE_POWER_LIMITATION =
      new ModbusRegister<>(
          30233, "PERMANENT_ACTIVE_POWER_LIMITATION", "Device status", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> TOTAL_ENERGY_FED =
      new ModbusRegister<>(
          30513,
          "TOTAL_ENERGY_FED",
          "Total energy fed in on all line conductors (in Wh)",
          U64,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> TOTAL_ENERGY_FED_DAILY =
      new ModbusRegister<>(
          30517,
          "TOTAL_ENERGY_FED_DAILY",
          "Energy fed in on the current day on all line conductors (daily yield) (Wh)",
          U64,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> OPERATING_TIME =
      new ModbusRegister<>(30521, "OPERATING_TIME", "Operating time (s)", U64, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> FEED_IN_TIME =
      new ModbusRegister<>(30525, "FEED_IN_TIME", "Feed-in time (s)", U64, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> PURCHASED_ELECTRICITY_TODAY =
      new ModbusRegister<>(
          30577,
          "PURCHASED_ELECTRICITY_TODAY",
          "Purchased electricity today",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> CURRENT_ACTIVE_POWER =
      new ModbusRegister<>(
          30775,
          "CURRENT_ACTIVE_POWER",
          "Total active power on all line conductors (W)",
          S32,
          FIX0,
          READ_ONLY);

  public static final ModbusRegister<BigDecimal> AMBIENT_TEMPERATURE =
      new ModbusRegister<>(
          34609, "AMBIENT_TEMPERATURE", "Ambient temperature (ºC)", S32, TEMP, READ_ONLY);

  public static final ModbusRegister<Number> CURRENT_BATTERY_STATE_OF_CHARGE =
      new ModbusRegister<>(
          30845,
          "CURRENT_BATTERY_STATE_OF_CHARGE",
          "Current battery state of charge (%)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> CURRENT_BATTERY_CAPACITY =
      new ModbusRegister<>(
          30847, "CURRENT_BATTERY_CAPACITY", "Current battery capacity (%)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<String> ACTIVE_BATTERY_CHARGING_MODE =
      new ModbusRegister<>(
          30853,
          "ACTIVE_BATTERY_CHARGING_MODE",
          "Active battery charging mode",
          U32,
          ENUM,
          READ_ONLY);
  public static final ModbusRegister<Number> POWER_PURCHASED_ELECTRICITY =
      new ModbusRegister<>(
          30865,
          "POWER_PURCHASED_ELECTRICITY",
          "Power purchased electricity (W)",
          S32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> POWER_GRID_FEED_IN =
      new ModbusRegister<>(
          30867, "POWER_GRID_FEED_IN", "Power grid feed-in (W)", S32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> POWER_PV_GENERATION =
      new ModbusRegister<>(
          30869, "POWER_PV_GENERATION", "Power PV generation (W)", S32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> CURRENT_SELF_CONSUMPTION =
      new ModbusRegister<>(
          30871, "CURRENT_SELF_CONSUMPTION", "Current self-consumption (W)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> CURRENT_INCREASED_SELF_CONSUMPTION =
      new ModbusRegister<>(
          30873,
          "CURRENT_INCREASED_SELF_CONSUMPTION",
          "Current self-consumption (W)",
          S32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> POWER_EXTERNAL_GRID_CONNECTION =
      new ModbusRegister<>(
          30885,
          "POWER_EXTERNAL_GRID_CONNECTION",
          "Power of external grid connection (W)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> PV_POWER =
      new ModbusRegister<>(30983, "PV_POWER", "PV power (W)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<BigDecimal> BATTERY_CHARGE_FACTOR =
      new ModbusRegister<>(
          30993,
          "BATTERY_CHARGE_FACTOR",
          "Charge factor: Ratio battery charging/-discharging",
          U32,
          FIX3,
          READ_ONLY);
  public static final ModbusRegister<Number> BATTERY_REMAINING_TIME_FULL_CHARGE =
      new ModbusRegister<>(
          31003,
          "BATTERY_REMAINING_TIME_FULL_CHARGE",
          "Remaining time until full charge (s)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> TOTAL_PV_ENERGY =
      new ModbusRegister<>(31063, "TOTAL_PV_ENERGY", "Total PV energy (Wh)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> TOTAL_PV_ENERGY_TODAY =
      new ModbusRegister<>(
          31065, "TOTAL_PV_ENERGY_TODAY", "Total PV energy today (Wh)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> BATTERY_NUMBER_OF_FULL_CHARGES =
      new ModbusRegister<>(
          31069,
          "BATTERY_NUMBER_OF_FULL_CHARGES",
          "Number of full charges of the battery",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> PV_ENERGY_PRODUCED_TODAY =
      new ModbusRegister<>(
          31091,
          "PV_ENERGY_PRODUCED_TODAY",
          "PV energy produced (today) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> PV_ENERGY_PRODUCED_YESTERDAY =
      new ModbusRegister<>(
          31093,
          "PV_ENERGY_PRODUCED_YESTERDAY",
          "PV energy produced (yesterday) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> PV_ENERGY_PRODUCED_CURRENT_MONTH =
      new ModbusRegister<>(
          31095,
          "PV_ENERGY_PRODUCED_CURRENT_MONTH",
          "PV energy produced (current month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> PV_ENERGY_PRODUCED_PREVIOUS_MONTH =
      new ModbusRegister<>(
          31097,
          "PV_ENERGY_PRODUCED_PREVIOUS_MONTH",
          "PV energy produced (previous month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> GRID_FEED_IN_TODAY =
      new ModbusRegister<>(
          31107, "GRID_FEED_IN_TODAY", "Grid feed-in today (Wh)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> ENERGY_FEED_IN_YESTERDAY =
      new ModbusRegister<>(
          31109,
          "ENERGY_FEED_IN_YESTERDAY",
          "Energy fed into the utility grid (yesterday) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> ENERGY_FEED_IN_CURRENT_MONTH =
      new ModbusRegister<>(
          31111,
          "ENERGY_FEED_IN_CURRENT_MONTH",
          "Energy fed into the utility grid (current month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> ENERGY_FEED_IN_PREVIOUS_MONTH =
      new ModbusRegister<>(
          31113,
          "ENERGY_FEED_IN_PREVIOUS_MONTH",
          "Energy fed into the utility grid (previous month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> ENERGY_DRAWN_FROM_YESTERDAY =
      new ModbusRegister<>(
          31115,
          "ENERGY_DRAWN_FROM_YESTERDAY",
          "Energy drawn from the utility grid (yesterday) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> ENERGY_DRAWN_FROM_CURRENT_MONTH =
      new ModbusRegister<>(
          31117,
          "ENERGY_DRAWN_FROM_CURRENT_MONTH",
          "Energy drawn from the utility grid (current month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> ENERGY_DRAWN_FROM_PREVIOUS_MONTH =
      new ModbusRegister<>(
          31119,
          "ENERGY_DRAWN_FROM_PREVIOUS_MONTH",
          "Energy drawn from the utility grid (previous month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> CONSUMED_ENERGY_TODAY =
      new ModbusRegister<>(
          31121, "CONSUMED_ENERGY_TODAY", "Consumed energy (today) (Wh)", U32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> CONSUMED_ENERGY_YESTERDAY =
      new ModbusRegister<>(
          31123,
          "CONSUMED_ENERGY_YESTERDAY",
          "Consumed energy (yesterday) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> CONSUMED_ENERGY_CURRENT_MONTH =
      new ModbusRegister<>(
          31125,
          "CONSUMED_ENERGY_CURRENT_MONTH",
          "Consumed energy (current month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> CONSUMED_ENERGY_PREVIOUS_MONTH =
      new ModbusRegister<>(
          31127,
          "CONSUMED_ENERGY_PREVIOUS_MONTH",
          "Consumed energy (previous month) (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> UNUSED_PV_POWER =
      new ModbusRegister<>(
          31129,
          "UNUSED_PV_POWER",
          "Unused PV power (W)",
          S32, // Documented as U32 but changed to S32 by real received values
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> AVAILABLE_PV_POWER =
      new ModbusRegister<>(
          31131, "AVAILABLE_PV_POWER", "Available PV power (W)", S32, FIX0, READ_ONLY);
  public static final ModbusRegister<Number> INTERNAL_PV_POWER_LIMITATION =
      new ModbusRegister<>(
          31133,
          "INTERNAL_PV_POWER_LIMITATION",
          "Internal PV power limitation (W)",
          S32,
          FIX0,
          READ_ONLY);

  public static final ModbusRegister<BigDecimal> PV_POWER_LIMITATION_COMUNICATION =
      new ModbusRegister<>(
          31239,
          "PV_POWER_LIMITATION_COMUNICATION",
          "PV power limitation via communication (in %)",
          U32,
          FIX2,
          READ_ONLY);

  public static final ModbusRegister<Number> BATTERY_CURRENT_CHARGING =
      new ModbusRegister<>(
          31393,
          "BATTERY_CURRENT_CHARGING",
          "Battery charging: current battery state of charge, in W",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> BATTERY_CURRENT_DISCHARGING =
      new ModbusRegister<>(
          31395,
          "BATTERY_CURRENT_DISCHARGING",
          "Battery charging: current battery state of charge, in W",
          U32,
          FIX0,
          READ_ONLY);

  public static final ModbusRegister<Number> NOMINAL_CAPACITY_BATTERY =
      new ModbusRegister<>(
          40187,
          "NOMINAL_CAPACITY_BATTERY",
          "Nominal capacity of the battery (Wh)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> MAXIMUM_CHARGE_BATTERY_POWER =
      new ModbusRegister<>(
          40189,
          "MAXIMUM_CHARGE_BATTERY_POWER",
          "Maximum charge power of the battery (W)",
          U32,
          FIX0,
          READ_ONLY);
  public static final ModbusRegister<Number> MAXIMUM_DISCHARGE_BATTERY_POWER =
      new ModbusRegister<>(
          40191,
          "MAXIMUM_DISCHARGE_BATTERY_POWER",
          "Maximum discharge power of the battery (W)",
          U32,
          FIX0,
          READ_ONLY);

  private String name;
  private int registerNumber;
  private String desription;
  private ModbusDataType dataType;
  private ModbusDataFormat<T> dataFormat;
  private ModbusAccesType accesType;
  private Class type;

  public ModbusRegister(
      int registerNumber,
      String name,
      String desription,
      ModbusDataType dataType,
      ModbusDataFormat<T> dataFormat,
      ModbusAccesType accesType) {
    this.registerNumber = registerNumber;
    this.name = name;
    this.desription = desription;
    this.dataType = dataType;
    this.dataFormat = dataFormat;
    this.type = type;
    this.accesType = accesType;
  }

  public int getRegisterNumber() {
    return registerNumber;
  }

  public String getName() {
    return name;
  }

  public String getDesription() {
    return desription;
  }

  public ModbusDataType getDataType() {
    return dataType;
  }

  public ModbusDataFormat<T> getDataFormat() {
    return dataFormat;
  }

  public ModbusAccesType getAccesType() {
    return accesType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ModbusRegister<?> register = (ModbusRegister<?>) o;
    return Objects.equals(name, register.name);
  }

  @Override
  public int hashCode() {

    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
