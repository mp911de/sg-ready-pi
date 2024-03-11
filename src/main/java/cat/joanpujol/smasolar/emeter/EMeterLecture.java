package cat.joanpujol.smasolar.emeter;

/** EMeter reading model */
public class EMeterLecture {
  private String deviceServiceNumber;
  private long ticker;

  private Data currentSum = new Data();
  private Data currentPhase1 = new Data();
  private Data currentPhase2 = new Data();
  private Data currentPhase3 = new Data();

  private Data counterSum = new Data();
  private Data counterPhase1 = new Data();
  private Data counterPhase2 = new Data();
  private Data counterPhase3 = new Data();

  public static class Data {
    private double activePower;
    private double negativeActivePower;

    private double reactivePower;
    private double negativeReactivePower;

    private double apparentPower;
    private double negativeApparentPower;

    private double powerFactor;
    private double electricCurrent;
    private double voltage;

    public double getActivePower() {
      return activePower;
    }

    public void setActivePower(double activePower) {
      this.activePower = activePower;
    }

    public double getNegativeActivePower() {
      return negativeActivePower;
    }

    public void setNegativeActivePower(double negativeActivePower) {
      this.negativeActivePower = negativeActivePower;
    }

    public double getReactivePower() {
      return reactivePower;
    }

    public void setReactivePower(double reactivePower) {
      this.reactivePower = reactivePower;
    }

    public double getNegativeReactivePower() {
      return negativeReactivePower;
    }

    public void setNegativeReactivePower(double negativeReactivePower) {
      this.negativeReactivePower = negativeReactivePower;
    }

    public double getApparentPower() {
      return apparentPower;
    }

    public void setApparentPower(double apparentPower) {
      this.apparentPower = apparentPower;
    }

    public double getNegativeApparentPower() {
      return negativeApparentPower;
    }

    public void setNegativeApparentPower(double negativeApparentPower) {
      this.negativeApparentPower = negativeApparentPower;
    }

    public double getPowerFactor() {
      return powerFactor;
    }

    public void setPowerFactor(double powerFactor) {
      this.powerFactor = powerFactor;
    }

    public double getElectricCurrent() {
      return electricCurrent;
    }

    public void setElectricCurrent(double electricCurrent) {
      this.electricCurrent = electricCurrent;
    }

    public double getVoltage() {
      return voltage;
    }

    public void setVoltage(double voltage) {
      this.voltage = voltage;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Data{");
      sb.append("activePower=").append(activePower);
      sb.append(", negativeActivePower=").append(negativeActivePower);
      sb.append(", reactivePower=").append(reactivePower);
      sb.append(", negativeReactivePower=").append(negativeReactivePower);
      sb.append(", apparentPower=").append(apparentPower);
      sb.append(", negativeApparentPower=").append(negativeApparentPower);
      sb.append(", powerFactor=").append(powerFactor);
      sb.append(", electricCurrent=").append(electricCurrent);
      sb.append(", voltage=").append(voltage);
      sb.append('}');
      return sb.toString();
    }
  }

  public String getDeviceServiceNumber() {
    return deviceServiceNumber;
  }

  public void setDeviceServiceNumber(String deviceServiceNumber) {
    this.deviceServiceNumber = deviceServiceNumber;
  }

  public long getTicker() {
    return ticker;
  }

  public void setTicker(long ticker) {
    this.ticker = ticker;
  }

  public Data getCurrentSum() {
    return currentSum;
  }

  public void setCurrentSum(Data currentSum) {
    this.currentSum = currentSum;
  }

  public Data getCurrentPhase1() {
    return currentPhase1;
  }

  public void setCurrentPhase1(Data currentPhase1) {
    this.currentPhase1 = currentPhase1;
  }

  public Data getCurrentPhase2() {
    return currentPhase2;
  }

  public void setCurrentPhase2(Data currentPhase2) {
    this.currentPhase2 = currentPhase2;
  }

  public Data getCurrentPhase3() {
    return currentPhase3;
  }

  public void setCurrentPhase3(Data currentPhase3) {
    this.currentPhase3 = currentPhase3;
  }

  public Data getCounterSum() {
    return counterSum;
  }

  public void setCounterSum(Data counterSum) {
    this.counterSum = counterSum;
  }

  public Data getCounterPhase1() {
    return counterPhase1;
  }

  public void setCounterPhase1(Data counterPhase1) {
    this.counterPhase1 = counterPhase1;
  }

  public Data getCounterPhase2() {
    return counterPhase2;
  }

  public void setCounterPhase2(Data counterPhase2) {
    this.counterPhase2 = counterPhase2;
  }

  public Data getCounterPhase3() {
    return counterPhase3;
  }

  public void setCounterPhase3(Data counterPhase3) {
    this.counterPhase3 = counterPhase3;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("EMeterReading{");
    sb.append("deviceServiceNumber='").append(deviceServiceNumber).append('\'');
    sb.append(", ticker=").append(ticker);
    sb.append(", currentSum=").append(currentSum);
    sb.append(", currentPhase1=").append(currentPhase1);
    sb.append(", currentPhase2=").append(currentPhase2);
    sb.append(", currentPhase3=").append(currentPhase3);
    sb.append(", counterSum=").append(counterSum);
    sb.append(", counterPhase1=").append(counterPhase1);
    sb.append(", counterPhase2=").append(counterPhase2);
    sb.append(", counterPhase3=").append(counterPhase3);
    sb.append('}');
    return sb.toString();
  }
}
