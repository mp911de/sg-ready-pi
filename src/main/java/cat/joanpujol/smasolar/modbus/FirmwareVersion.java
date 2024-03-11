package cat.joanpujol.smasolar.modbus;

import java.util.Objects;

public class FirmwareVersion {
  enum ReleaseType {
    N(0, "No revision number"),
    E(1, "Experimental release"),
    A(2, "Alpha release"),
    B(3, "Beta release"),
    R(4, "Release"),
    S(5, "Special release"),
    NUMBER(-1, "No special interpretation");

    private int numberCode;
    private String description;

    ReleaseType(int numberCode, String description) {
      this.numberCode = numberCode;
      this.description = description;
    }

    public static ReleaseType fromNumberCode(int numberCode) {
      switch (numberCode) {
        case 0:
          return N;
        case 1:
          return E;
        case 2:
          return A;
        case 3:
          return B;
        case 4:
          return R;
        case 5:
          return S;
        default:
          return NUMBER;
      }
    }
  }

  private int major;
  private int minor;
  private int build;
  private ReleaseType releaseType;

  public FirmwareVersion(int major, int minor, int build, ReleaseType releaseType) {
    this.major = major;
    this.minor = minor;
    this.build = build;
    this.releaseType = releaseType;
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getBuild() {
    return build;
  }

  public ReleaseType getReleaseType() {
    return releaseType;
  }

  @Override
  public String toString() {
    return String.format("%d.%02d.%d.%s", major, minor, build, releaseType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FirmwareVersion that = (FirmwareVersion) o;
    return major == that.major
        && minor == that.minor
        && build == that.build
        && releaseType == that.releaseType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, build, releaseType);
  }
}
